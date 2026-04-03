package com.noa99kee.board.post.controller;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.noa99kee.board.auth.principal.BoardUserPrincipal;
import com.noa99kee.board.common.ImageUploadValidation;
import com.noa99kee.board.config.UploadRootHolder;
import com.noa99kee.board.post.dto.CreatePostRequest;
import com.noa99kee.board.post.dto.PostDetailResponse;
import com.noa99kee.board.post.dto.PostListItemResponse;
import com.noa99kee.board.post.dto.UpdatePostRequest;
import com.noa99kee.board.post.service.PostService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

/**
 * 게시글 CRUD 및 본문용 이미지 업로드 API입니다.
 *
 * <p>목록·상세는 인증 없이 열려 있고(SecurityConfig에서 permitAll), 작성·수정·삭제·이미지 업로드는 JWT(Bearer)가 필요합니다.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

	private static final Logger log = LoggerFactory.getLogger(PostController.class);
	private static final long MAX_POST_IMAGE_BYTES = 5L * 1024 * 1024;

	private final PostService postService;
	private final UploadRootHolder uploadRootHolder;

	public PostController(PostService postService, UploadRootHolder uploadRootHolder) {
		this.postService = postService;
		this.uploadRootHolder = uploadRootHolder;
	}

	@GetMapping
	public List<PostListItemResponse> list() {
		return postService.findAllForList();
	}

	@GetMapping("/{id}")
	public PostDetailResponse detail(@PathVariable UUID id) {
		return postService.findOne(id);
	}

	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public PostDetailResponse create(
			@AuthenticationPrincipal BoardUserPrincipal principal, @Valid @RequestBody CreatePostRequest dto) {
		return postService.create(dto, principal.id());
	}

	@PatchMapping("/{id}")
	public PostDetailResponse update(
			@AuthenticationPrincipal BoardUserPrincipal principal,
			@PathVariable UUID id,
			@Valid @RequestBody UpdatePostRequest dto) {
		return postService.update(id, dto, principal.id());
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal BoardUserPrincipal principal, @PathVariable UUID id) {
		postService.remove(id, principal.id());
		return ResponseEntity.noContent().build();
	}

	@PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Map<String, String> uploadImage(
			@AuthenticationPrincipal BoardUserPrincipal principal, @RequestPart("file") MultipartFile file) {
		ImageUploadValidation.requireValidImage(file, MAX_POST_IMAGE_BYTES);
		String ext = ImageUploadValidation.fileExtension(file.getOriginalFilename());
		String filename = UUID.randomUUID() + ext;
		try {
			Path target = uploadRootHolder.postsDir().resolve(filename).normalize();
			if (!target.startsWith(uploadRootHolder.postsDir())) {
				throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "잘못된 파일명입니다.");
			}
			file.transferTo(target.toFile());
		} catch (Exception e) {
			throw new ResponseStatusException(
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "저장에 실패했습니다.");
		}
		log.info("posts.image.upload userId={} stored={}", principal.id(), filename);
		return Map.of("url", PostService.POST_IMAGE_PUBLIC_PREFIX + filename);
	}
}
