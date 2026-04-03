package com.noa99kee.board.user.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 회원 테이블({@code users})에 매핑되는 JPA 엔티티입니다.
 *
 * <p>비밀번호는 {@code passwordHash}에만 두고, 리프레시 토큰도 DB에는 해시만 저장합니다.
 *
 * <p><b>클래스 어노테이션</b> — {@link com.noa99kee.board.post.entity.Post}와 동일하게 {@code @Entity}, {@code @Table},
 * {@code @EntityListeners(AuditingEntityListener.class)}로 테이블 매핑·생성·수정 시각 자동 채움을 씁니다.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** 로그인 식별자. {@code unique = true}로 DB 유니크 제약을 겁니다. */
	@Column(nullable = false, unique = true, length = 320)
	private String email;

	/** BCrypt 등으로 해시된 비밀번호만 저장합니다. */
	@Column(name = "password_hash", nullable = false, length = 120)
	private String passwordHash;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(name = "profile_image_url", length = 512)
	private String profileImageUrl;

	/** 리프레시 토큰 원문이 아니라 해시만 보관합니다. */
	@Column(name = "refresh_token_hash", length = 64)
	private String refreshTokenHash;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public void setProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public String getRefreshTokenHash() {
		return refreshTokenHash;
	}

	public void setRefreshTokenHash(String refreshTokenHash) {
		this.refreshTokenHash = refreshTokenHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
