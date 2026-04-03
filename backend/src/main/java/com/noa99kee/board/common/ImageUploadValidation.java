package com.noa99kee.board.common;

import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * 컨트롤러에서 반복되던 이미지 업로드 검증(비어 있음, Content-Type, 용량)을 한곳에 모았습니다.
 *
 * <p>실무에서는 파일 저장·바이러스 검사·오브젝트 스토리지(S3 등) 연동까지 확장하는 경우가 많습니다.
 */
public final class ImageUploadValidation {

	/** 브라우저가 보내는 MIME 타입 기준 허용 목록(확장자만 믿지 않음). */
	public static final Set<String> ALLOWED_IMAGE_TYPES =
			Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

	private ImageUploadValidation() {}

	/**
	 * 멀티파트 파일이 비어 있지 않고, 허용된 이미지 타입이며, 최대 크기 이하인지 검사합니다.
	 *
	 * @param maxBytes 허용 최대 바이트(예: 프로필 2MiB, 게시글 5MiB)
	 */
	public static void requireValidImage(MultipartFile file, long maxBytes) {
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 필요합니다.");
		}
		String ct = file.getContentType();
		if (ct == null || !ALLOWED_IMAGE_TYPES.contains(ct)) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "이미지(jpeg, png, webp, gif)만 업로드할 수 있습니다.");
		}
		if (file.getSize() > maxBytes) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 너무 큽니다.");
		}
	}

	/** 원본 파일명에서 확장자만 추출합니다. 없으면 {@code .bin}을 씁니다. */
	public static String fileExtension(String originalFilename) {
		if (originalFilename == null) {
			return ".bin";
		}
		int i = originalFilename.lastIndexOf('.');
		if (i < 0) {
			return ".bin";
		}
		return originalFilename.substring(i).toLowerCase();
	}
}
