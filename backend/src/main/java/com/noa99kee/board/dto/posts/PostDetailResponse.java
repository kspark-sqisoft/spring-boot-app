package com.noa99kee.board.dto.posts;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 상세 화면용: 본문과 이미지 URL 배열을 포함합니다. */
public record PostDetailResponse(
		String id,
		String title,
		String content,
		@JsonProperty("createdAt") Instant createdAt,
		@JsonProperty("updatedAt") Instant updatedAt,
		@JsonProperty("authorId") String authorId,
		@JsonProperty("authorName") String authorName,
		@JsonProperty("imageUrls") List<String> imageUrls) {}
