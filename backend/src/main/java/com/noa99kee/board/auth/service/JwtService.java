package com.noa99kee.board.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.noa99kee.board.config.JwtConfig;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/** 액세스 토큰(JWT) 발급·파싱만 담당합니다. 리프레시 토큰은 쿠키+DB 해시로 {@link AuthService}에서 처리합니다. */
@Service
public class JwtService {

	private final SecretKey accessKey;
	private final int accessExpirationMinutes;

	public JwtService(JwtConfig jwtConfig) {
		byte[] keyBytes = jwtConfig.accessSecret().getBytes(StandardCharsets.UTF_8);
		if (keyBytes.length < 32) {
			throw new IllegalStateException(
					"JWT access secret must be at least 32 bytes (UTF-8). Set JWT_ACCESS_SECRET.");
		}
		this.accessKey = Keys.hmacShaKeyFor(keyBytes);
		this.accessExpirationMinutes = jwtConfig.accessExpirationMinutes();
	}

	public String createAccessToken(UUID userId, String email) {
		Instant now = Instant.now();
		Instant exp = now.plusSeconds(accessExpirationMinutes * 60L);
		return Jwts.builder()
				.subject(userId.toString())
				.claim("email", email)
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(accessKey)
				.compact();
	}

	public Claims parseAccessTokenClaims(String token) {
		return Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token).getPayload();
	}
}
