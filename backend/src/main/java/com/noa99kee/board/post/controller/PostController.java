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
 *
 * <p><b>클래스 어노테이션</b>
 *
 * <ul>
 *   <li>{@code @RestController} — 반환값을 뷰 이름이 아니라 HTTP 응답 본문(JSON)으로 직렬화합니다. ({@code @Controller} + 모든
 *       메서드에 {@code @ResponseBody}를 붙인 것과 같습니다.)
 *   <li>{@code @RequestMapping("/api/posts")} — 이 컨트롤러 안의 {@code @GetMapping} 등에 공통 경로 접두사 {@code /api/posts}를
 *       붙입니다.
 * </ul>
 *
 * <p><b>메서드·매개변수 어노테이션 (자주 쓰는 것)</b>
 *
 * <ul>
 *   <li>{@code @GetMapping} / {@code @PostMapping} / {@code @PatchMapping} / {@code @DeleteMapping} — HTTP 메서드와 URL
 *       조각을 지정합니다. 클래스의 {@code @RequestMapping}과 이어 붙여 최종 경로가 됩니다.
 *   <li>{@code consumes = MediaType...} — 요청의 {@code Content-Type}이 지정한 값일 때만 이 핸들러로 라우팅합니다.
 *   <li>{@code @PathVariable} — URL 경로 변수({@code /{id}})를 메서드 인자(예: {@code UUID})로 바인딩합니다.
 *   <li>{@code @RequestBody} — 요청 본문(JSON)을 DTO 객체로 역직렬화합니다.
 *   <li>{@code @Valid} — DTO에 붙은 Jakarta Validation 제약({@code @NotBlank}, {@code @Size} 등)을 이 지점에서 검사합니다.
 *       실패 시 보통 400과 검증 오류 메시지가 나갑니다.
 *   <li>{@code @AuthenticationPrincipal} — Spring Security가 SecurityContext에 넣어 둔 “현재 로그인 사용자”를 주입합니다.
 *       이 프로젝트에서는 JWT 필터가 토큰을 검증한 뒤 {@link BoardUserPrincipal}을 설정합니다. 비인증 요청이면 {@code null}일 수
 *       있으나, 이 컨트롤러의 보호된 메서드는 Security에서 막히므로 null이 오지 않습니다.
 *   <li>{@code @RequestPart("file")} — {@code multipart/form-data} 요청에서 이름이 {@code file}인 파트를 {@link MultipartFile}로
 *       받습니다.
 *   <li>{@code ResponseEntity} — HTTP 상태 코드·헤더·본문을 함께 제어할 때 씁니다. 예: 삭제 후 본문 없이 204.
 * </ul>
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

	/** {@code GET /api/posts} — 전체 목록(인증 불필요). */
	@GetMapping
	public List<PostListItemResponse> list() {
		return postService.findAllForList();
	}

	/** {@code GET /api/posts/{id}} — 단건 상세(인증 불필요). {@code @PathVariable}로 글 id를 받습니다. */
	@GetMapping("/{id}")
	public PostDetailResponse detail(@PathVariable UUID id) {
		return postService.findOne(id);
	}

	/**
	 * {@code POST /api/posts} — 글 작성(JSON 본문). {@code consumes}로 {@code application/json}만 허용. 로그인 사용자 id는
	 * {@code @AuthenticationPrincipal}로 주입합니다.
	 */
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

	/** {@code DELETE /api/posts/{id}} — 삭제 후 {@code 204 No Content}({@code ResponseEntity#noContent}). */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal BoardUserPrincipal principal, @PathVariable UUID id) {
		postService.remove(id, principal.id());
		return ResponseEntity.noContent().build();
	}

	/**
	 * {@code POST /api/posts/images} — 본문 삽입용 이미지 파일 업로드. {@code multipart/form-data}의 {@code file} 파트만
	 * {@code @RequestPart}로 받습니다.
	 */
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
