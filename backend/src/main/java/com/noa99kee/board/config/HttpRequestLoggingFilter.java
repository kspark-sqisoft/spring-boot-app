package com.noa99kee.board.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청이 들어올 때와 응답이 나갈 때(상태 코드·소요 시간·로그인 주체)를 로그로 남깁니다.
 *
 * <p>헬스 체크는 로그가 너무 많아질 수 있어 DEBUG 레벨로만 남깁니다.
 */
@Component
public class HttpRequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(HttpRequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		long startNs = System.nanoTime();
		String method = request.getMethod();
		String path = request.getRequestURI();
		String query = request.getQueryString();
		String pathWithQuery = query == null ? path : path + "?" + query;
		boolean quiet =
				path.startsWith("/api/health")
						|| path.startsWith("/swagger-ui")
						|| path.startsWith("/v3/api-docs");

		if (quiet) {
			log.debug("HTTP -> {} {}", method, pathWithQuery);
		} else {
			log.info("HTTP -> {} {}", method, pathWithQuery);
		}

		try {
			filterChain.doFilter(request, response);
		} finally {
			long ms = (System.nanoTime() - startNs) / 1_000_000L;
			int status = response.getStatus();
			String principal = resolvePrincipal();
			if (quiet) {
				log.debug(
						"HTTP <- {} {} status={} {}ms principal={}",
						method,
						pathWithQuery,
						status,
						ms,
						principal);
			} else {
				log.info(
						"HTTP <- {} {} status={} {}ms principal={}",
						method,
						pathWithQuery,
						status,
						ms,
						principal);
			}
		}
	}

	private static String resolvePrincipal() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null
				|| !auth.isAuthenticated()
				|| auth instanceof AnonymousAuthenticationToken) {
			return "anonymous";
		}
		return auth.getName();
	}
}
