package com.noa99kee.board.post.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.noa99kee.board.user.entity.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 게시글(posts) JPA 엔티티. User와 다대일.
 *
 * <p>클래스 어노테이션: Entity(영속), Table(name=posts), EntityListeners(AuditingEntityListener)로 생성·수정 시각 자동
 * 채움. BoardApplication의 EnableJpaAuditing과 함께 동작.
 */
@Entity
@Table(name = "posts")
@EntityListeners(AuditingEntityListener.class)
public class Post {

	/** PK. Id + GeneratedValue(UUID). */
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** 제목. Column nullable false, length 200. */
	@Column(nullable = false, length = 200)
	private String title;

	/** 본문. Column columnDefinition text. */
	@Column(nullable = false, columnDefinition = "text")
	private String content;

	/** 이미지 URL 목록. JdbcTypeCode(구조화 컬럼) + Column image_urls. */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "image_urls", nullable = false)
	private List<String> imageUrls = new ArrayList<>();

	/**
	 * 작성자. ManyToOne(LAZY), JoinColumn(author_id), OnDelete(SET_NULL).
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id")
	@OnDelete(action = OnDeleteAction.SET_NULL)
	private User author;

	/** 생성 시각. CreatedDate, Column updatable false. */
	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	/** 수정 시각. LastModifiedDate. */
	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public List<String> getImageUrls() {
		return imageUrls;
	}

	public void setImageUrls(List<String> imageUrls) {
		this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
	}

	public User getAuthor() {
		return author;
	}

	public void setAuthor(User author) {
		this.author = author;
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
