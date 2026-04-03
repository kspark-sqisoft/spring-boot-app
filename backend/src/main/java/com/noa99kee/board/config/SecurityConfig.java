package com.noa99kee.board.config;

import com.noa99kee.board.security.JwtAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security: 어떤 URL을 익명 허용할지, 세션을 쓸지, 어떤 필터를 끼워 넣을지 정의합니다.
 *
 * <p>이 프로젝트는 JWT(Bearer) + httpOnly 리프레시 쿠키를 쓰므로 세션은 끕니다({@link
 * org.springframework.security.config.http.SessionCreationPolicy#STATELESS}),
 * {@link com.noa99kee.board.security.JwtAuthenticationFilter}에서 액세스 토큰을 검사합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final HttpRequestLoggingFilter httpRequestLoggingFilter;

	public SecurityConfig(
			JwtAuthenticationFilter jwtAuthenticationFilter,
			HttpRequestLoggingFilter httpRequestLoggingFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.httpRequestLoggingFilter = httpRequestLoggingFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		// REST API + JWT에서는 CSRF 토큰을 브라우저 폼에 실어 보내기 어려워 보통 비활성화합니다(대신 쿠키 SameSite 등으로 완화).
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(Customizer.withDefaults());
		http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.authorizeHttpRequests(
				auth ->
						auth.requestMatchers(HttpMethod.GET, "/api/health", "/api/health/**")
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
								(request, response, authException) ->
										response.sendError(
												HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized")));
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
