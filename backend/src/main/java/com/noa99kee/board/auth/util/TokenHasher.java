package com.noa99kee.board.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 리프레시 토큰은 DB에 평문이 아니라 SHA-256 해시로만 저장합니다. 탈취되어도 원문 복구가 어렵습니다.
 *
 * <p>무작위 원문 토큰은 URL-safe Base64로 생성합니다.
 */
public final class TokenHasher {

	private static final SecureRandom RANDOM = new SecureRandom();

	private TokenHasher() {}

	public static String newRawRefreshToken() {
		byte[] buf = new byte[48];
		RANDOM.nextBytes(buf);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
	}

	public static String sha256Hex(String raw) {
		try {
			MessageDigest d = MessageDigest.getInstance("SHA-256");
			byte[] h = d.digest(raw.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(h.length * 2);
			for (byte b : h) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
