package com.noa99kee.board.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code application.yml}의 {@code app.auth.*} 값을 자바 객체로 바인딩합니다. */
@ConfigurationProperties(prefix = "app.auth")
public record AuthConfig(String refreshCookieName, int refreshExpiresDays, boolean refreshCookieSecure) {}
