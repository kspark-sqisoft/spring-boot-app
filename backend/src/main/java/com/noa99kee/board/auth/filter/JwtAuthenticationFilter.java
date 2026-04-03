package com.noa99kee.board.auth.filter;

import java.io.IOException;
import java.util.UUID;

import com.noa99kee.board.auth.principal.BoardUserPrincipal;
import com.noa99kee.board.auth.service.JwtService;
import com.noa99kee.board.user.entity.User;
import com.noa99kee.board.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

/**
 * HTTP {@code Authorization: Bearer ...} 헤더의 액세스 JWT를 검증하고, 성공 시 Spring Security
 * {@link org.springframework.security.core.context.SecurityContext}에 인증 객체를 넣습니다.
 *
 * <p>리프레시 토큰은 httpOnly 쿠키로만 다루고 이 필터에서는 다루지 않습니다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
		this.jwtService = jwtService;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}
		String token = header.substring(7).trim();
		if (token.isEmpty()) {
			filterChain.doFilter(request, response);
			return;
		}
		try {
			Claims claims = jwtService.parseAccessTokenClaims(token);
			UUID userId = UUID.fromString(claims.getSubject());
			User user =
					userRepository.findById(userId).orElseThrow(() -> new JwtException("user not found"));
			BoardUserPrincipal principal =
					new BoardUserPrincipal(user.getId(), user.getEmail(), user.getName());
			var auth = new UsernamePasswordAuthenticationToken(
					principal, null, principal.getAuthorities());
			auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(auth);
		} catch (JwtException | IllegalArgumentException ex) {
			SecurityContextHolder.clearContext();
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired access token");
			return;
		}
		filterChain.doFilter(request, response);
	}
}
