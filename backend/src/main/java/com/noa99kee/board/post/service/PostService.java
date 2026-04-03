package com.noa99kee.board.post.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.noa99kee.board.config.UploadRootHolder;
import com.noa99kee.board.post.dto.CreatePostRequest;
import com.noa99kee.board.post.dto.PostDetailResponse;
import com.noa99kee.board.post.dto.PostListItemResponse;
import com.noa99kee.board.post.dto.UpdatePostRequest;
import com.noa99kee.board.post.entity.Post;
import com.noa99kee.board.post.repository.PostRepository;
import com.noa99kee.board.user.entity.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 게시글 목록·상세 조회, 작성·수정·삭제와 첨부 이미지 URL 검증·디스크 정리를 담당합니다.
 *
 * <p>작성자만 수정·삭제 가능 같은 규칙은 엔티티를 불러온 뒤 여기서 검사합니다.
 *
 * <p><b>어노테이션</b>
 *
 * <ul>
 *   <li>{@code @Service} — 스프링이 컴포넌트 스캔 대상으로 등록하는 서비스(비즈니스) 계층 빈입니다. {@code @Component}와 같이
 *       싱글톤으로 컨테이너에 올라갑니다.
 *   <li>{@code @Transactional} — 해당 메서드(또는 클래스 전체)를 하나의 트랜잭션으로 감쌉니다. DB 읽기·쓰기가 한 번에 커밋/롤백되며,
 *       기본적으로 RuntimeException 시 롤백됩니다. 조회 전용 메서드에는 생략하거나 읽기 전용 최적화를 붙이기도 합니다.
 * </ul>
 */
@Service
public class PostService {

	private static final Logger log = LoggerFactory.getLogger(PostService.class);
	public static final String POST_IMAGE_PUBLIC_PREFIX = "/uploads/posts/";
	private static final int MAX_POST_IMAGES = 5;

	private final PostRepository postRepository;
	private final UploadRootHolder uploadRootHolder;

	public PostService(PostRepository postRepository, UploadRootHolder uploadRootHolder) {
		this.postRepository = postRepository;
		this.uploadRootHolder = uploadRootHolder;
	}

	public List<PostListItemResponse> findAllForList() {
		List<PostListItemResponse> list =
				postRepository.findAllForList().stream().map(this::toListItem).toList();
		log.debug("posts.list count={}", list.size());
		return list;
	}

	public PostDetailResponse findOne(UUID id) {
		Post post = postRepository.findDetailById(id).orElseThrow(() -> notFound(id));
		log.debug("posts.detail id={}", id);
		return toDetail(post);
	}

	@Transactional
	public PostDetailResponse create(CreatePostRequest dto, UUID authorId) {
		List<String> urls = sanitizeImageUrls(dto.imageUrls());
		Post p = new Post();
		p.setTitle(dto.title().trim());
		p.setContent(dto.content());
		User author = new User();
		author.setId(authorId);
		p.setAuthor(author);
		p.setImageUrls(new ArrayList<>(urls));
		Post saved = postRepository.save(p);
		log.info("posts.create id={} authorId={} title={}", saved.getId(), authorId, saved.getTitle());
		return findOne(saved.getId());
	}

	@Transactional
	public PostDetailResponse update(UUID id, UpdatePostRequest dto, UUID userId) {
		if (dto.title() == null && dto.content() == null && dto.imageUrls() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "수정할 제목·내용 또는 첨부 이미지가 필요합니다.");
		}
		Post post = postRepository.findById(id).orElseThrow(() -> notFound(id));
		if (post.getAuthor() == null || !post.getAuthor().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 글만 수정할 수 있습니다.");
		}
		if (dto.title() != null) {
			post.setTitle(dto.title().trim());
		}
		if (dto.content() != null) {
			post.setContent(dto.content());
		}
		if (dto.imageUrls() != null) {
			List<String> next = sanitizeImageUrls(dto.imageUrls());
			List<String> prev = new ArrayList<>(post.getImageUrls());
			for (String u : prev) {
				if (!next.contains(u)) {
					unlinkPostImageFile(u);
				}
			}
			post.setImageUrls(new ArrayList<>(next));
		}
		postRepository.save(post);
		log.info("posts.update id={} userId={}", id, userId);
		return findOne(id);
	}

	@Transactional
	public void remove(UUID id, UUID userId) {
		Post post = postRepository.findById(id).orElseThrow(() -> notFound(id));
		if (post.getAuthor() == null || !post.getAuthor().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 글만 삭제할 수 있습니다.");
		}
		for (String u : post.getImageUrls()) {
			unlinkPostImageFile(u);
		}
		postRepository.delete(post);
		log.info("posts.delete id={} userId={}", id, userId);
	}

	public List<String> sanitizeImageUrls(List<String> raw) {
		if (raw == null) {
			return List.of();
		}
		if (raw.size() > MAX_POST_IMAGES) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST, "이미지는 최대 " + MAX_POST_IMAGES + "장까지 첨부할 수 있습니다.");
		}
		Set<String> seen = new HashSet<>();
		for (String u : raw) {
			if (u == null || !u.startsWith(POST_IMAGE_PUBLIC_PREFIX)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 이미지 경로입니다.");
			}
			String name = u.substring(POST_IMAGE_PUBLIC_PREFIX.length());
			if (name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 이미지 경로입니다.");
			}
			if (!seen.add(u)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "중복된 이미지 경로가 있습니다.");
			}
		}
		return List.copyOf(seen);
	}

	private void unlinkPostImageFile(String url) {
		Path p = diskPathForPostImageUrl(url);
		if (p == null) {
			return;
		}
		try {
			Files.deleteIfExists(p);
		} catch (Exception ignored) {
			// best-effort
		}
	}

	private Path diskPathForPostImageUrl(String url) {
		if (!url.startsWith(POST_IMAGE_PUBLIC_PREFIX)) {
			return null;
		}
		String name = url.substring(POST_IMAGE_PUBLIC_PREFIX.length());
		if (name.isEmpty() || name.contains("..") || name.contains("/") || name.contains("\\")) {
			return null;
		}
		Path base = uploadRootHolder.postsDir();
		Path candidate = base.resolve(name).normalize();
		if (!candidate.startsWith(base)) {
			return null;
		}
		return candidate;
	}

	private PostListItemResponse toListItem(Post p) {
		return new PostListItemResponse(
				p.getId().toString(),
				p.getTitle(),
				p.getCreatedAt(),
				p.getAuthor() != null ? p.getAuthor().getId().toString() : null,
				p.getAuthor() != null ? p.getAuthor().getName() : null);
	}

	private PostDetailResponse toDetail(Post p) {
		return new PostDetailResponse(
				p.getId().toString(),
				p.getTitle(),
				p.getContent(),
				p.getCreatedAt(),
				p.getUpdatedAt(),
				p.getAuthor() != null ? p.getAuthor().getId().toString() : null,
				p.getAuthor() != null ? p.getAuthor().getName() : null,
				new ArrayList<>(p.getImageUrls()));
	}

	private static ResponseStatusException notFound(UUID id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "Post " + id + " not found");
	}
}
