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
 *
 * <p><b>어노테이션</b>
 *
 * <ul>
 *   <li>{@code @RestController} — 반환 {@code Map}을 JSON 객체로 직렬화합니다.
 *   <li>{@code @RequestMapping("/api/health")} — 기본 경로.
 *   <li>{@code @GetMapping} (경로 생략) — {@code GET /api/health}에 대응합니다. 추가 경로를 주지 않으면 클래스 레벨 경로만 사용합니다.
 * </ul>
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

	/** {@code GET /api/health} — 상태 문자열과 타임스탬프만 반환합니다. */
	@GetMapping
	public Map<String, Object> check() {
		return Map.of("status", "ok", "timestamp", Instant.now().toString());
	}
}
