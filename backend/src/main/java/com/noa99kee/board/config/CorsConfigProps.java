package com.noa99kee.board.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 비어 있으면 CORS 패턴 {@code *}(개발 편의), 값이 있으면 쉼표로 구분한 Origin만 허용합니다. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsConfigProps(String allowedOrigins) {}
