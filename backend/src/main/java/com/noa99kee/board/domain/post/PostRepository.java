package com.noa99kee.board.domain.post;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Spring Data JPA가 구현체를 자동 생성합니다. 메서드 이름 규칙 대신 {@code @Query}로 JPQL을 씁니다. */
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
