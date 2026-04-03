package com.noa99kee.board.user.controller;

import java.nio.file.Path;
import java.util.UUID;

import com.noa99kee.board.auth.dto.UpdateProfileRequest;
import com.noa99kee.board.auth.principal.BoardUserPrincipal;
import com.noa99kee.board.auth.service.AuthService;
import com.noa99kee.board.common.ImageUploadValidation;
import com.noa99kee.board.config.UploadRootHolder;
import com.noa99kee.board.user.dto.UserResponse;
import com.noa99kee.board.user.service.UserService;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

/**
 * 현재 로그인 사용자 프로필 API({@code /api/auth/me})입니다. URL은 프론트 계약을 위해 auth 경로를 유지합니다.
 *
 * <p><b>어노테이션</b>
 *
 * <ul>
 *   <li>{@code @RestController} / {@code @RequestMapping("/api/auth")} — {@link com.noa99kee.board.auth.controller.AuthController}와
 *       같은 접두사 아래 {@code /me} 등으로 프로필 전용 엔드포인트를 둡니다.
 *   <li>{@code @GetMapping("/me")} — GET 단건 조회.
 *   <li>{@code @PatchMapping("/me")} — 이름 등 부분 수정(JSON). {@code @Valid @RequestBody}로 검증.
 *   <li>{@code @PostMapping(..., consumes = MULTIPART_FORM_DATA_VALUE)} — 아바타만 multipart로 받기 위해 Content-Type을 한정합니다.
 *   <li>{@code @AuthenticationPrincipal} — 현재 사용자({@link BoardUserPrincipal}). Security에서 인증된 요청만 허용되므로 null이 아닙니다.
 *   <li>{@code @RequestPart("file")} — 폼 필드 이름 {@code file}에 해당하는 업로드 파트.
 * </ul>
 */
@RestController
@RequestMapping("/api/auth")
public class UserController {

	private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

	private final UserService userService;
	private final AuthService authService;
	private final UploadRootHolder uploadRootHolder;

	public UserController(UserService userService, AuthService authService, UploadRootHolder uploadRootHolder) {
		this.userService = userService;
		this.authService = authService;
		this.uploadRootHolder = uploadRootHolder;
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal BoardUserPrincipal principal) {
		return userService.toResponse(userService.requireById(principal.id()));
	}

	@PatchMapping("/me")
	public UserResponse updateMe(
			@AuthenticationPrincipal BoardUserPrincipal principal, @Valid @RequestBody UpdateProfileRequest dto) {
		return authService.updateName(principal, dto.name());
	}

	@PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UserResponse uploadAvatar(
			@AuthenticationPrincipal BoardUserPrincipal principal, @RequestPart("file") MultipartFile file) {
		ImageUploadValidation.requireValidImage(file, MAX_AVATAR_BYTES);
		String ext = ImageUploadValidation.fileExtension(file.getOriginalFilename());
		String filename = UUID.randomUUID() + ext;
		try {
			Path target = uploadRootHolder.profilesDir().resolve(filename).normalize();
			if (!target.startsWith(uploadRootHolder.profilesDir())) {
				throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "잘못된 파일명입니다.");
			}
			file.transferTo(target.toFile());
		} catch (Exception e) {
			throw new ResponseStatusException(
					org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "저장에 실패했습니다.");
		}
		return authService.saveAvatarFile(principal, filename, uploadRootHolder.root());
	}
}
