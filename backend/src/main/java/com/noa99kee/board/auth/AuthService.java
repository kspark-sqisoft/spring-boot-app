package com.noa99kee.board.auth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import com.noa99kee.board.config.AuthConfig;
import com.noa99kee.board.domain.user.User;
import com.noa99kee.board.domain.user.UserRepository;
import com.noa99kee.board.application.user.UserService;
import com.noa99kee.board.dto.auth.AuthPayload;
import com.noa99kee.board.dto.auth.LoginRequest;
import com.noa99kee.board.dto.auth.RegisterRequest;
import com.noa99kee.board.dto.user.UserResponse;
import com.noa99kee.board.security.BoardUserPrincipal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 회원가입·로그인·리프레시 쿠키 로테이션·로그아웃·프로필(이름·아바타 URL 반영) 등 인증·세션에 가까운 흐름을 모읍니다.
 *
 * <p>단순 사용자 필드 갱신은 {@link com.noa99kee.board.application.user.UserService}에 두고, 여기서는 토큰·쿠키와 맞물리는 부분을
 * 처리합니다. DB에 쓰기가 있는 메서드에는 {@code @Transactional}을 붙여 한 요청 안에서 일관되게 커밋되게 합니다.
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);
	private static final String PROFILE_PREFIX = "/uploads/profiles/";

	private final UserRepository userRepository;
	private final UserService userService;
	private final JwtService jwtService;
	private final PasswordEncoder passwordEncoder;
	private final AuthConfig authConfig;

	public AuthService(
			UserRepository userRepository,
			UserService userService,
			JwtService jwtService,
			PasswordEncoder passwordEncoder,
			AuthConfig authConfig) {
		this.userRepository = userRepository;
		this.userService = userService;
		this.jwtService = jwtService;
		this.passwordEncoder = passwordEncoder;
		this.authConfig = authConfig;
	}

	public AuthPayload register(RegisterRequest dto, HttpServletResponse response) {
		String email = dto.email().toLowerCase().trim();
		if (userRepository.findByEmail(email).isPresent()) {
			log.warn("auth.register conflict email={}", email);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다.");
		}
		String hash = passwordEncoder.encode(dto.password());
		User user = userService.create(email, hash, dto.name().trim());
		AuthPayload out = issueSession(user, response);
		log.info("auth.register ok userId={} email={}", user.getId(), email);
		return out;
	}

	public AuthPayload login(LoginRequest dto, HttpServletResponse response) {
		String email = dto.email().toLowerCase().trim();
		User user = userRepository.findByEmail(email).orElse(null);
		if (user == null) {
			log.warn("auth.login failed (unknown email) email={}", email);
			throw unauthorizedLogin();
		}
		if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
			log.warn("auth.login failed (bad password) email={}", email);
			throw unauthorizedLogin();
		}
		AuthPayload out = issueSession(user, response);
		log.info("auth.login ok userId={} email={}", user.getId(), email);
		return out;
	}

	private static ResponseStatusException unauthorizedLogin() {
		return new ResponseStatusException(
				HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다.");
	}

	@Transactional
	public AuthPayload refreshTokens(HttpServletRequest request, HttpServletResponse response) {
		String raw = readRefreshCookie(request);
		if (raw == null) {
			log.warn("auth.refresh rejected (no cookie)");
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "리프레시 토큰이 없습니다.");
		}
		String hash = TokenHasher.sha256Hex(raw);
		User user = userRepository
				.findByRefreshTokenHash(hash)
				.orElseThrow(
						() -> {
							log.warn("auth.refresh rejected (invalid token)");
							clearRefreshCookie(response);
							return new ResponseStatusException(
									HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다.");
						});
		String rotated = TokenHasher.newRawRefreshToken();
		user.setRefreshTokenHash(TokenHasher.sha256Hex(rotated));
		userRepository.save(user);
		attachRefreshCookie(response, rotated);
		User fresh = userRepository.findById(user.getId()).orElse(user);
		String access = jwtService.createAccessToken(fresh.getId(), fresh.getEmail());
		log.info("auth.refresh ok userId={}", fresh.getId());
		return new AuthPayload(access, userService.toResponse(fresh));
	}

	@Transactional
	public void logout(BoardUserPrincipal principal, HttpServletResponse response) {
		userService.setRefreshTokenHash(principal.id(), null);
		clearRefreshCookie(response);
		log.info("auth.logout userId={}", principal.id());
	}

	@Transactional
	public UserResponse updateName(BoardUserPrincipal principal, String name) {
		UserResponse u = userService.updateName(principal.id(), name);
		log.info("auth.profile.name userId={}", principal.id());
		return u;
	}

	@Transactional
	public UserResponse saveAvatarFile(BoardUserPrincipal principal, String newFilename, Path uploadsRoot) {
		User user = userService.requireById(principal.id());
		String prev = user.getProfileImageUrl();
		Path profilesDir = uploadsRoot.resolve("profiles").normalize();
		if (prev != null && prev.startsWith(PROFILE_PREFIX)) {
			String fn = prev.substring(PROFILE_PREFIX.length());
			if (safeSegment(fn)) {
				try {
					Files.deleteIfExists(profilesDir.resolve(fn).normalize());
				} catch (Exception ignored) {
					// best-effort
				}
			}
		}
		UserResponse u = userService.setProfileImageUrl(principal.id(), PROFILE_PREFIX + newFilename);
		log.info("auth.profile.avatar userId={} file={}", principal.id(), newFilename);
		return u;
	}

	private static boolean safeSegment(String fn) {
		return !fn.isEmpty() && !fn.contains("..") && !fn.contains("/") && !fn.contains("\\");
	}

	private AuthPayload issueSession(User user, HttpServletResponse response) {
		String raw = TokenHasher.newRawRefreshToken();
		user.setRefreshTokenHash(TokenHasher.sha256Hex(raw));
		userRepository.save(user);
		attachRefreshCookie(response, raw);
		String access = jwtService.createAccessToken(user.getId(), user.getEmail());
		return new AuthPayload(access, userService.toResponse(user));
	}

	private String readRefreshCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		String name = authConfig.refreshCookieName();
		for (Cookie c : cookies) {
			if (name.equals(c.getName())) {
				return c.getValue();
			}
		}
		return null;
	}

	private void attachRefreshCookie(HttpServletResponse response, String rawRefresh) {
		ResponseCookie cookie = ResponseCookie.from(authConfig.refreshCookieName(), rawRefresh)
				.httpOnly(true)
				.secure(authConfig.refreshCookieSecure())
				.sameSite("Lax")
				.path("/api/auth")
				.maxAge(Duration.ofDays(authConfig.refreshExpiresDays()))
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

	private void clearRefreshCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(authConfig.refreshCookieName(), "")
				.httpOnly(true)
				.secure(authConfig.refreshCookieSecure())
				.sameSite("Lax")
				.path("/api/auth")
				.maxAge(0)
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}
}
