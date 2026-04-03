package com.noa99kee.board.post.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.noa99kee.board.post.entity.Post;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA가 런타임에 프록시 구현체를 만들어 주입합니다.
 *
 * <p><b>어노테이션·상속</b>
 *
 * <ul>
 *   <li>{@code extends JpaRepository&lt;Post, UUID&gt;} — {@code Post} 엔티티, 기본키 타입 {@code UUID}. {@code save}, {@code findById},
 *       {@code delete} 등 CRUD가 기본 제공됩니다.
 *   <li>{@code @Query("…")} — 메서드 이름 파생 쿼리 대신 JPQL 문자열을 직접 지정합니다. {@code select} 구문은 엔티티 필드명을 씁니다(DB
 *       컬럼명이 아님).
 *   <li>{@code @Param("id")} — JPQL 이름 파라미터 {@code :id}와 메서드 인자를 연결합니다. 이름이 다르면 바인딩이 어긋납니다.
 * </ul>
 */
public interface PostRepository extends JpaRepository<Post, UUID> {

	/** 목록에서 작성자 이름을 바로 쓰기 위해 author를 한 번에 불러옵니다(fetch join, N+1 완화). */
	@Query(
			"""
			select distinct p from Post p
			left join fetch p.author
			order by p.createdAt desc
			""")
	List<Post> findAllForList();

	/** 단건 상세도 작성자 정보를 함께 로드합니다. */
	@Query(
			"""
			select distinct p from Post p
			left join fetch p.author
			where p.id = :id
			""")
	Optional<Post> findDetailById(@Param("id") UUID id);
}
