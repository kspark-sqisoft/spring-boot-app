package com.noa99kee.board.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

/** 업로드 루트 디렉터리와 그 아래 {@code profiles}, {@code posts} 폴더 경로를 애플리케이션 전역에서 씁니다. */
@Component
public class UploadRootHolder {

	private final Path root;

	public UploadRootHolder(UploadConfig uploadConfig) throws IOException {
		this.root = Paths.get(uploadConfig.dir()).toAbsolutePath().normalize();
		Files.createDirectories(root.resolve("profiles"));
		Files.createDirectories(root.resolve("posts"));
	}

	public Path root() {
		return root;
	}

	public Path profilesDir() {
		return root.resolve("profiles");
	}

	public Path postsDir() {
		return root.resolve("posts");
	}

	public Path postFile(String filename) {
		return postsDir().resolve(filename).normalize();
	}
}
