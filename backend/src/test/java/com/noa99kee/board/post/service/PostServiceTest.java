package com.noa99kee.board.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.noa99kee.board.config.UploadRootHolder;
import com.noa99kee.board.post.dto.CreatePostRequest;
import com.noa99kee.board.post.dto.UpdatePostRequest;
import com.noa99kee.board.post.entity.Post;
import com.noa99kee.board.post.repository.PostRepository;
import com.noa99kee.board.user.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import com.noa99kee.board.post.dto.PostDetailResponse;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostServiceTest {

	@Mock
	private PostRepository postRepository;

	@Mock
	private UploadRootHolder uploadRootHolder;

	private PostService postService;

	@BeforeEach
	void setUp(@TempDir java.nio.file.Path temp) throws Exception {
		when(uploadRootHolder.postsDir()).thenReturn(temp.resolve("posts"));
		postService = new PostService(postRepository, uploadRootHolder);
	}

	@Test
	@DisplayName("sanitizeImageUrls: null 이면 빈 리스트")
	void sanitizeNull() {
		assertThat(postService.sanitizeImageUrls(null)).isEmpty();
	}

	@Test
	@DisplayName("sanitizeImageUrls: 허용 prefix 가 아니면 BAD_REQUEST")
	void sanitizeBadPrefix() {
		assertThatThrownBy(() -> postService.sanitizeImageUrls(List.of("http://evil.com/x.png")))
				.isInstanceOfSatisfying(
						ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode().value()).isEqualTo(400));
	}

	@Test
	@DisplayName("sanitizeImageUrls: 최대 장수 초과 시 BAD_REQUEST")
	void sanitizeTooMany() {
		List<String> urls = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			urls.add(PostService.POST_IMAGE_PUBLIC_PREFIX + "f" + i + ".png");
		}
		assertThatThrownBy(() -> postService.sanitizeImageUrls(urls))
				.isInstanceOfSatisfying(
						ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode().value()).isEqualTo(400));
	}

	@Test
	@DisplayName("create: 저장된 글 id 로 상세 조회까지 이어진다")
	void createDelegatesToRepository() {
		UUID authorId = UUID.randomUUID();
		UUID postId = UUID.randomUUID();
		User author = new User();
		author.setId(authorId);
		author.setEmail("a@b.com");
		author.setName("A");

		Post forDetail = new Post();
		forDetail.setId(postId);
		forDetail.setTitle("Title");
		forDetail.setContent("Body");
		forDetail.setImageUrls(new ArrayList<>());
		forDetail.setAuthor(author);

		when(postRepository.save(any(Post.class)))
				.thenAnswer(
						invocation -> {
							Post p = invocation.getArgument(0);
							p.setId(postId);
							return p;
						});
		when(postRepository.findDetailById(postId)).thenReturn(Optional.of(forDetail));

		PostDetailResponse result =
				postService.create(new CreatePostRequest("  Title  ", "Body", List.of()), authorId);

		assertThat(result.title()).isEqualTo("Title");
		assertThat(result.content()).isEqualTo("Body");
		verify(postRepository).save(any(Post.class));
	}

	@Test
	@DisplayName("update: 작성자가 아니면 FORBIDDEN")
	void updateForbiddenForNonAuthor() {
		UUID postId = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		UUID intruder = UUID.randomUUID();

		Post post = new Post();
		post.setId(postId);
		User author = new User();
		author.setId(owner);
		post.setAuthor(author);

		when(postRepository.findById(postId)).thenReturn(Optional.of(post));

		assertThatThrownBy(
						() ->
								postService.update(
										postId, new UpdatePostRequest("x", null, null), intruder))
				.isInstanceOfSatisfying(
						ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode().value()).isEqualTo(403));
	}

	@Test
	@DisplayName("update: 변경 필드가 하나도 없으면 BAD_REQUEST")
	void updateNothingToChange() {
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		Post post = new Post();
		post.setAuthor(new User());
		post.getAuthor().setId(userId);
		when(postRepository.findById(postId)).thenReturn(Optional.of(post));

		assertThatThrownBy(() -> postService.update(postId, new UpdatePostRequest(null, null, null), userId))
				.isInstanceOfSatisfying(
						ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode().value()).isEqualTo(400));
	}
}
