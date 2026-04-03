package com.noa99kee.board.user.service;

import java.util.UUID;

import com.noa99kee.board.user.dto.UserResponse;
import com.noa99kee.board.user.entity.User;
import com.noa99kee.board.user.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자(User) 엔티티에 대한 조회·생성·필드 갱신을 담당합니다(프로필·회원 레코드).
 *
 * <p>로그인·쿠키·JWT 발급 흐름은 {@link com.noa99kee.board.auth.service.AuthService}에 있습니다.
 */
@Service
public class UserService {

	private final UserRepository userRepository;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/** 엔티티를 API 응답용 레코드로 변환합니다. */
	public UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId().toString(),
				user.getEmail(),
				user.getName(),
				user.getProfileImageUrl(),
				user.getCreatedAt(),
				user.getUpdatedAt());
	}

	@Transactional
	public User create(String email, String passwordHash, String name) {
		User u = new User();
		u.setEmail(email);
		u.setPasswordHash(passwordHash);
		u.setName(name);
		u.setProfileImageUrl(null);
		u.setRefreshTokenHash(null);
		return userRepository.save(u);
	}

	public User requireById(UUID id) {
		return userRepository
				.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	@Transactional
	public void setRefreshTokenHash(UUID userId, String hashOrNull) {
		User u = requireById(userId);
		u.setRefreshTokenHash(hashOrNull);
	}

	@Transactional
	public UserResponse updateName(UUID userId, String name) {
		User u = requireById(userId);
		u.setName(name.trim());
		return toResponse(userRepository.save(u));
	}

	@Transactional
	public UserResponse setProfileImageUrl(UUID userId, String url) {
		User u = requireById(userId);
		u.setProfileImageUrl(url);
		return toResponse(userRepository.save(u));
	}
}
