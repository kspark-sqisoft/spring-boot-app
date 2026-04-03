package com.noa99kee.board.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.noa99kee.board.user.dto.UserResponse;

/** 로그인·회원가입·리프레시 성공 시 내려주는 액세스 토큰 + 사용자 요약 정보입니다. */
public record AuthPayload(
		@JsonProperty("accessToken") String accessToken,
		UserResponse user) {}
