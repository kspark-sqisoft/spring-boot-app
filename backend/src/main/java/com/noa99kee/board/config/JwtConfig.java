package com.noa99kee.board.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** JWT 액세스 토큰 서명 키와 만료 시간(분) 설정입니다. */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtConfig(String accessSecret, int accessExpirationMinutes) {}
