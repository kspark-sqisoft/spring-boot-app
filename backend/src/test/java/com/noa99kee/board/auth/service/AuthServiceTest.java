package com.noa99kee.board.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.noa99kee.board.auth.dto.LoginRequest;
import com.noa99kee.board.auth.dto.RegisterRequest;
import com.noa99kee.board.config.AuthConfig;
import com.noa99kee.board.user.entity.User;
import com.noa99kee.board.user.repository.UserRepository;
import com.noa99kee.board.user.service.UserService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserService userService;

	@Mock
	private JwtService jwtService;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private AuthConfig authConfig;

	@InjectMocks
	private AuthService authService;

	@Test
	@DisplayName("register: 이메일 중복이면 저장하지 않고 409")
	void registerDuplicateEmail() {
		when(userRepository.findByEmail("dup@example.com")).thenReturn(Optional.of(new User()));

		assertThatThrownBy(
						() ->
								authService.register(
										new RegisterRequest("dup@example.com", "password12", "N"),
										new MockHttpServletResponse()))
				.isInstanceOfSatisfying(
						ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode().value()).isEqualTo(409));

		verify(userService, never()).create(any(), any(), any());
	}

	@Test
	@DisplayName("login: 존재하지 않는 이메일이면 401")
	void loginUnknownEmail() {
		when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

		assertThatThrownBy(
						() ->
								authService.login(
										new LoginRequest("missing@example.com", "password12"),
										new MockHttpServletResponse()))
				.isInstanceOfSatisfying(
						ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode().value()).isEqualTo(401));

		verify(passwordEncoder, never()).matches(any(), any());
	}
}
