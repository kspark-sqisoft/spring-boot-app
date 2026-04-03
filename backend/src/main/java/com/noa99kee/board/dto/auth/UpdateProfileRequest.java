package com.noa99kee.board.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** {@code PATCH /api/auth/me} 로 표시 이름만 바꿀 때 쓰는 본문입니다. */
public record UpdateProfileRequest(@NotBlank @Size(max = 100) String name) {}
