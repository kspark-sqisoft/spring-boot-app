package com.noa99kee.board.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.noa99kee.board.config.JwtConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;

class JwtServiceTest {

	private JwtService jwtService;

	@BeforeEach
	void setUp() {
		jwtService = new JwtService(new JwtConfig("unit_test_jwt_secret_key_32bytes!!", 120));
	}

	@Test
	@DisplayName("발급한 토큰을 파싱하면 subject·email 클레임이 일치한다")
	void roundTrip() {
		UUID userId = UUID.randomUUID();
		String email = "jwt@example.com";
		String token = jwtService.createAccessToken(userId, email);

		Claims claims = jwtService.parseAccessTokenClaims(token);
		assertThat(claims.getSubject()).isEqualTo(userId.toString());
		assertThat(claims.get("email", String.class)).isEqualTo(email);
	}

	@Test
	@DisplayName("다른 키로 서명된 토큰은 파싱 실패")
	void wrongSignatureFails() {
		JwtService other = new JwtService(new JwtConfig("other_jwt_secret_key_32bytes_min!!", 60));
		String token = other.createAccessToken(UUID.randomUUID(), "x@y.com");

		assertThatThrownBy(() -> jwtService.parseAccessTokenClaims(token)).isInstanceOf(JwtException.class);
	}
}
