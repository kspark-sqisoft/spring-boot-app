package com.noa99kee.board.application.user;

import java.util.UUID;

import com.noa99kee.board.domain.user.User;
import com.noa99kee.board.domain.user.UserRepository;
import com.noa99kee.board.dto.user.UserResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 사용자(User) 엔티티에 대한 조회·생성·필드 갱신을 담당합니다.
 *
 * <p>로그인/쿠키/JWT 발급 같은 "인증 흐름"은 {@link com.noa99kee.board.auth.AuthService}에 두고, 여기서는 DB와 1:1에 가까운
 * 사용자 도메인 작업만 처리하는 편이 역할이 분리되어 테스트·유지보수에 유리합니다.
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
