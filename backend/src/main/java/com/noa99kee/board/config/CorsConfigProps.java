package com.noa99kee.board.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 비어 있으면 개발용 기본 Origin 목록을 쓰고, 값이 있으면 쉼표로 구분된 Origin을 허용합니다. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsConfigProps(String allowedOrigins) {}
