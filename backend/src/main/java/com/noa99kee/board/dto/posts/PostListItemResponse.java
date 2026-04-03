package com.noa99kee.board.dto.posts;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 목록 화면용: 본문·이미지 없이 제목·작성자·작성일만 내려 부하를 줄입니다. */
public record PostListItemResponse(
		String id,
		String title,
		@JsonProperty("createdAt") Instant createdAt,
		@JsonProperty("authorId") String authorId,
		@JsonProperty("authorName") String authorName) {}
