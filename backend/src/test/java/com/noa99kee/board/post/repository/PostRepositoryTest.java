package com.noa99kee.board.post.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.noa99kee.board.post.entity.Post;
import com.noa99kee.board.user.entity.User;
import com.noa99kee.board.user.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@ActiveProfiles("test")
class PostRepositoryTest {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TestEntityManager testEntityManager;

	@Test
	@DisplayName("findAllForList: 작성자와 함께 조회되고 최신 순으로 정렬된다")
	@Transactional
	void findAllForListOrdersByCreatedAtDesc() {
		User a = persistUser("a@example.com", "hash", "Author A");
		User b = persistUser("b@example.com", "hash", "Author B");

		Post older = new Post();
		older.setTitle("older");
		older.setContent("c1");
		older.setAuthor(a);
		postRepository.save(older);

		Post newer = new Post();
		newer.setTitle("newer");
		newer.setContent("c2");
		newer.setAuthor(b);
		postRepository.save(newer);
		postRepository.flush();

		List<Post> list = postRepository.findAllForList();
		assertThat(list).hasSize(2);
		assertThat(list).extracting(Post::getTitle).containsExactlyInAnyOrder("older", "newer");
		assertThat(list).allMatch(p -> p.getAuthor() != null);
		assertThat(list.stream().filter(p -> "newer".equals(p.getTitle())).findFirst().orElseThrow().getAuthor().getName())
				.isEqualTo("Author B");
	}

	@Test
	@DisplayName("findDetailById: 존재하면 author fetch 로 로드된다")
	@Transactional
	void findDetailByIdLoadsAuthor() {
		User u = persistUser("d@example.com", "hash", "D");
		Post p = new Post();
		p.setTitle("one");
		p.setContent("body");
		p.setAuthor(u);
		UUID id = postRepository.save(p).getId();
		postRepository.flush();
		testEntityManager.clear();

		var found = postRepository.findDetailById(id);
		assertThat(found).isPresent();
		assertThat(found.get().getAuthor().getEmail()).isEqualTo("d@example.com");
	}

	private User persistUser(String email, String hash, String name) {
		User u = new User();
		u.setEmail(email);
		u.setPasswordHash(hash);
		u.setName(name);
		return userRepository.save(u);
	}
}
