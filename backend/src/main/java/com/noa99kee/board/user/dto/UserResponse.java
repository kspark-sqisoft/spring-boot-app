package com.noa99kee.board.user.dto;

import java.time.Instant;

/**
 * 클라이언트(프론트)로 내려보내는 사용자 정보 JSON 형태입니다.
 *
 * <p>{@link com.noa99kee.board.user.entity.User} 엔티티와 달리 비밀번호 해시·리프레시 토큰 해시는 절대 포함하지 않습니다.
 */
public record UserResponse(
		String id,
		String email,
		String name,
		String profileImageUrl,
		Instant createdAt,
		Instant updatedAt) {}
