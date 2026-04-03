package com.noa99kee.board.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 로컬 디스크에 업로드 파일을 저장할 루트 경로입니다. */
@ConfigurationProperties(prefix = "app.uploads")
public record UploadConfig(String dir) {}
