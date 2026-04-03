package com.noa99kee.board.post.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 새 글 작성 시 본문. {@code imageUrls}는 업로드 API로 받은 공개 URL만 허용하고 서비스에서 한 번 더 검증합니다. */
public record CreatePostRequest(
		@NotBlank @Size(max = 200) String title,
		@NotBlank String content,
		@Size(max = 5) List<@NotBlank @Size(max = 512) String> imageUrls) {

	public CreatePostRequest {
		if (imageUrls == null) {
			imageUrls = List.of();
		}
	}
}
