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
			// credentials=true 일 때도 Spring은 패턴 "*" 에 대해 요청 Origin 을 그대로 반사합니다.
			// LAN IP·다른 호스트로 Vite 접속 시 고정 목록만으로는 403 Invalid CORS 가 납니다.
			c.addAllowedOriginPattern("*");
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
