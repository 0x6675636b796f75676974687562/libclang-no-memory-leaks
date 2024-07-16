package com.example;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;

import static com.example.ClangUtils.parse;
import static java.util.Objects.requireNonNull;

final class Main {
	private Main() {
		assert false;
	}

	public static void main(final String... args) throws URISyntaxException {
		final var codeUrl = requireNonNull(Main.class.getResource("/test.cc"));
		final var absoluteFile = Path.of(codeUrl.toURI()).toAbsolutePath().normalize();
		parse(absoluteFile, List.of("-std=gnu++20", "-fparse-all-comments"));
	}
}
