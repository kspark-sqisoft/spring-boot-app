package com.noa99kee.board.dto.posts;

import java.util.List;

import jakarta.validation.constraints.Size;

/** 부분 수정: 보낸 필드만 갱신합니다(JSON {@code null}이면 해당 필드는 건드리지 않음). */
public record UpdatePostRequest(
		@Size(max = 200) String title,
		String content,
		@Size(max = 5) List<@Size(max = 512) String> imageUrls) {}
