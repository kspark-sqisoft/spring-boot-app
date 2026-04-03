package com.noa99kee.board.api;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import com.noa99kee.board.application.user.UserService;
import com.noa99kee.board.auth.AuthService;
import com.noa99kee.board.dto.auth.AuthPayload;
import com.noa99kee.board.dto.auth.LoginRequest;
import com.noa99kee.board.dto.auth.RegisterRequest;
import com.noa99kee.board.dto.auth.UpdateProfileRequest;
import com.noa99kee.board.dto.user.UserResponse;
import com.noa99kee.board.common.ImageUploadValidation;
import com.noa99kee.board.config.UploadRootHolder;
import com.noa99kee.board.security.BoardUserPrincipal;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * 인증·프로필 관련 HTTP API입니다.
 *
 * <p>{@code @RestController}는 메서드 반환값을 JSON으로 직렬화하고, {@code @RequestMapping}으로 이 클래스 전체 URL 접두사를
 * {@code /api/auth}로 맞춥니다. 실제 비즈니스 규칙은 {@link AuthService}·{@link UserService}에 있습니다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

	private final AuthService authService;
	private final UserService userService;
	private final UploadRootHolder uploadRootHolder;

	public AuthController(AuthService authService, UserService userService, UploadRootHolder uploadRootHolder) {
		this.authService = authService;
		this.userService = userService;
		this.uploadRootHolder = uploadRootHolder;
	}

	@PostMapping("/register")
	public AuthPayload register(@Valid @RequestBody RegisterRequest dto, HttpServletResponse response) {
		return authService.register(dto, response);
	}

	@PostMapping("/login")
	public AuthPayload login(@Valid @RequestBody LoginRequest dto, HttpServletResponse response) {
		return authService.login(dto, response);
	}

	@PostMapping("/logout")
	public Map<String, Boolean> logout(
			@AuthenticationPrincipal BoardUserPrincipal principal, HttpServletResponse response) {
		authService.logout(principal, response);
		return Map.of("ok", true);
	}

	/** httpOnly 리프레시 쿠키를 읽어 새 액세스 토큰을 발급합니다(쿠키 로테이션). */
	@PostMapping("/refresh")
	public AuthPayload refresh(HttpServletRequest request, HttpServletResponse response) {
		return authService.refreshTokens(request, response);
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal BoardUserPrincipal principal) {
		return userService.toResponse(userService.requireById(principal.id()));
	}

	@PatchMapping("/me")
	public UserResponse updateMe(
			@AuthenticationPrincipal BoardUserPrincipal principal, @Valid @RequestBody UpdateProfileRequest dto) {
		return authService.updateName(principal, dto.name());
	}

	@PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserResponse uploadAvatar(
			@AuthenticationPrincipal BoardUserPrincipal principal, @RequestPart("file") MultipartFile file) {
		ImageUploadValidation.requireValidImage(file, MAX_AVATAR_BYTES);
		String ext = ImageUploadValidation.fileExtension(file.getOriginalFilename());
		String filename = UUID.randomUUID() + ext;
		try {
			Path target = uploadRootHolder.profilesDir().resolve(filename).normalize();
			if (!target.startsWith(uploadRootHolder.profilesDir())) {
				throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "잘못된 파일명입니다.");
			}
			file.transferTo(target.toFile());
		} catch (Exception e) {
			throw new ResponseStatusException(
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "저장에 실패했습니다.");
		}
		return authService.saveAvatarFile(principal, filename, uploadRootHolder.root());
	}
}
