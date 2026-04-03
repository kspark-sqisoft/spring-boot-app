package com.noa99kee.board;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * 스프링 부트 애플리케이션 시작 클래스입니다.
 *
 * <ul>
 *   <li>{@code @SpringBootApplication} — 설정·컴포넌트 스캔·자동 구성을 한 번에 켭니다.
 *   <li>{@code @ConfigurationPropertiesScan} — {@code app.*} 같은 커스텀 설정 record를 빈으로 등록합니다.
 *   <li>{@code @EnableJpaAuditing} — 엔티티의 생성·수정 시각을 {@code @CreatedDate} 등으로 자동 채웁니다.
 * </ul>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
public class BoardApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoardApplication.class, args);
	}

}
