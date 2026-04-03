package com.noa99kee.board.user.repository;

import java.util.Optional;
import java.util.UUID;

import com.noa99kee.board.user.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 이메일·리프레시 토큰 해시 등으로 사용자를 찾습니다. */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	@Query(
			"""
			select u from User u
			where u.refreshTokenHash = :hash
			""")
	Optional<User> findByRefreshTokenHash(@Param("hash") String hash);
}
