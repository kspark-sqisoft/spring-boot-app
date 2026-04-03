package com.noa99kee.board.health.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로드밸런서·Docker 헬스체크·모니터링용으로 쓰는 가벼운 엔드포인트입니다.
 *
 * <p>DB 연결 검사까지 넣으면 더 정확하지만, 이 프로젝트에서는 단순히 애플리케이션이 살아 있는지만 확인합니다.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

	@GetMapping
	public Map<String, Object> check() {
		return Map.of("status", "ok", "timestamp", Instant.now().toString());
	}
}
