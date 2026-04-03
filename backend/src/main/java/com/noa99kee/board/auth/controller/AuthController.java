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

/** 회원가입·로그인·로그아웃·리프레시(쿠키) API입니다. 프로필 조회·수정은 {@link com.noa99kee.board.user.controller.UserController}입니다. */
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
