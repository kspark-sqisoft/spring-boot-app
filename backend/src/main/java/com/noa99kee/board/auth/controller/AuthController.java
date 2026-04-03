package com.noa99kee.board.auth.controller;

import java.util.Map;

import com.noa99kee.board.auth.dto.AuthPayload;
import com.noa99kee.board.auth.dto.LoginRequest;
import com.noa99kee.board.auth.dto.RegisterRequest;
import com.noa99kee.board.auth.principal.BoardUserPrincipal;
import com.noa99kee.board.auth.service.AuthService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * 회원가입·로그인·로그아웃·리프레시(쿠키) API입니다. 프로필 조회·수정은 {@link com.noa99kee.board.user.controller.UserController}입니다.
 *
 * <p><b>어노테이션</b>
 *
 * <ul>
 *   <li>{@code @RestController} — JSON 응답용 REST 컨트롤러({@code @Controller} + 본문 직렬화).
 *   <li>{@code @RequestMapping("/api/auth")} — 공통 URL 접두사.
 *   <li>{@code @PostMapping("/…")} — POST 전용 하위 경로.
 *   <li>{@code @RequestBody} + {@code @Valid} — JSON 본문을 DTO로 받고 Bean Validation 수행.
 *   <li>{@code HttpServletResponse} — 리프레시 토큰을 httpOnly 쿠키로 심기 위해 서블릿 응답 객체를 주입받습니다(어노테이션 없음,
 *       MVC가 타입만 보고 해결).
 *   <li>{@code HttpServletRequest} — 쿠키에서 리프레시 토큰을 읽기 위해 요청 객체 주입.
 *   <li>{@code @AuthenticationPrincipal} — 로그아웃 시 “누가 로그아웃하는지” 식별({@link BoardUserPrincipal}).
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
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

	@PostMapping("/refresh")
	public AuthPayload refresh(HttpServletRequest request, HttpServletResponse response) {
		return authService.refreshTokens(request, response);
	}
}
