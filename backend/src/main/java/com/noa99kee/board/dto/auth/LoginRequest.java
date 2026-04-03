package com.noa99kee.board.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code POST /api/auth/login} 요청 본문. */
public record LoginRequest(
		@NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 100) String password) {}
