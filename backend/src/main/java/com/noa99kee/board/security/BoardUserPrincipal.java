package com.noa99kee.board.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 로그인된 사용자를 나타내는 최소 정보입니다. {@link UserDetails}를 구현해 Spring Security와 호환됩니다.
 *
 * <p>비밀번호 필드는 JWT 인증에서는 쓰이지 않아 빈 문자열을 반환합니다.
 */
public record BoardUserPrincipal(UUID id, String email, String name) implements UserDetails {

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

	@Override
	public String getPassword() {
		return "";
	}

	@Override
	public String getUsername() {
		return email;
	}
}
