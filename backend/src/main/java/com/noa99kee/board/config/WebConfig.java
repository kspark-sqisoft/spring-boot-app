package com.noa99kee.board.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적 리소스(업로드 파일) URL 매핑과 브라우저 CORS 설정을 담당합니다.
 *
 * <p>{@code WebMvcConfigurer}를 구현해 스프링 MVC 동작을 조금씩 커스터마이즈합니다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final UploadRootHolder uploadRootHolder;
	private final CorsConfigProps corsConfigProps;

	public WebConfig(UploadRootHolder uploadRootHolder, CorsConfigProps corsConfigProps) {
		this.uploadRootHolder = uploadRootHolder;
		this.corsConfigProps = corsConfigProps;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String location = uploadRootHolder.root().toAbsolutePath().toUri().toString();
		if (!location.endsWith("/")) {
			location += "/";
		}
		registry.addResourceHandler("/uploads/**").addResourceLocations(location);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration c = new CorsConfiguration();
		String origins = corsConfigProps.allowedOrigins();
		if (origins == null || origins.isBlank()) {
			c.setAllowedOrigins(
					List.of(
							"http://localhost:5173",
							"http://127.0.0.1:5173",
							"http://localhost:8080",
							"http://127.0.0.1:8080",
							"http://localhost:3000",
							"http://127.0.0.1:3000"));
		} else {
			c.setAllowedOrigins(
					Arrays.stream(origins.split(","))
							.map(String::trim)
							.filter(s -> !s.isEmpty())
							.toList());
		}
		c.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
		c.setAllowCredentials(true);
		c.setAllowedHeaders(List.of("*"));
		c.setExposedHeaders(List.of("Set-Cookie"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", c);
		return source;
	}
}
