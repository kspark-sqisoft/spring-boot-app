package com.noa99kee.board.config;

import com.noa99kee.board.auth.filter.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security: 어떤 URL을 익명 허용할지, 세션을 쓸지, 어떤 필터를 끼워 넣을지 정의합니다.
 *
 * <p>이 프로젝트는 JWT(Bearer) + httpOnly 리프레시 쿠키를 쓰므로 세션은 끕니다({@link
 * org.springframework.security.config.http.SessionCreationPolicy#STATELESS}),
 * {@link com.noa99kee.board.auth.filter.JwtAuthenticationFilter}에서 액세스 토큰을 검사합니다.
 *
 * <p><b>클래스 어노테이션</b>
 *
 * <ul>
 *   <li>{@code @Configuration} — 이 클래스 안의 {@code @Bean} 메서드 정의를 스프링 설정으로 등록합니다.
 *   <li>{@code @EnableWebSecurity} — Spring Security 웹 필터 체인을 활성화합니다. {@code SecurityFilterChain} 빈이 없으면 기본 폼
 *       로그인 등이 켜지므로, 여기서 커스텀 체인을 반드시 정의합니다.
 * </ul>
 *
 * <p><b>{@code @Bean} 메서드</b>
 *
 * <ul>
 *   <li>{@code securityFilterChain(HttpSecurity)} — HTTP 보안 규칙(CORS, CSRF, 세션 정책, URL별 인증, 필터 순서)을 한 번에 구성해
 *       {@link SecurityFilterChain} 빈으로 등록합니다. 메서드 이름은 관례일 뿐이며, 빈 타입으로 주입됩니다.
 *   <li>{@code passwordEncoder()} — 비밀번호 해시용 {@link PasswordEncoder} 구현체(여기서는 BCrypt)를 빈으로 노출해, 서비스에서
 *       주입받아 사용합니다.
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final HttpRequestLoggingFilter httpRequestLoggingFilter;
	private final CorsConfigurationSource corsConfigurationSource;

	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			HttpRequestLoggingFilter httpRequestLoggingFilter,
			CorsConfigurationSource corsConfigurationSource) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.httpRequestLoggingFilter = httpRequestLoggingFilter;
		this.corsConfigurationSource = corsConfigurationSource;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// REST API + JWT에서는 CSRF 토큰을 브라우저 폼에 실어 보내기 어려워 보통 비활성화합니다(대신 쿠키 SameSite 등으로 완화).
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(c -> c.configurationSource(corsConfigurationSource));
		http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests(
				auth ->
						auth.requestMatchers(HttpMethod.OPTIONS, "/**")
								.permitAll()
								.requestMatchers(
										"/swagger-ui.html",
										"/swagger-ui/**",
										"/v3/api-docs",
										"/v3/api-docs/**")
								.permitAll()
								.requestMatchers(HttpMethod.GET, "/api/health", "/api/health/**")
								.permitAll()
								.requestMatchers(
										HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh")
								.permitAll()
								.requestMatchers(HttpMethod.GET, "/api/posts")
								.permitAll()
								.requestMatchers(HttpMethod.GET, "/api/posts/*")
								.permitAll()
								.requestMatchers("/uploads/**")
								.permitAll()
								.anyRequest()
								.authenticated());
		http.exceptionHandling(
				ex ->
						ex.authenticationEntryPoint(
								(request, response, authException) -> {
									if (request.getRequestURI().startsWith("/api/")) {
										response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
										response.setCharacterEncoding("UTF-8");
										response.setContentType("application/json;charset=UTF-8");
										response.getWriter().write(
												"{\"statusCode\":401,\"message\":\"인증이 필요합니다.\"}");
									} else {
										response.sendError(
												HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
									}
								}));
		// JWT 필터를 먼저 등록한 뒤, 그 앞에 요청 로깅 필터를 둡니다(Spring Security 7은 기준 필터가 먼저 체인에 있어야 함).
		http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		http.addFilterBefore(httpRequestLoggingFilter, JwtAuthenticationFilter.class);
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		// 비밀번호는 평문으로 저장하지 않고 BCrypt 해시만 DB에 둡니다. strength 10은 기본에 가까운 비용 계수입니다.
		return new BCryptPasswordEncoder(10);
	}
}
