package com.example;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.example.ClangUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

final class Main {
	private Main() {
		assert false;
	}

	public static void main(final String... args) throws URISyntaxException {
		final URL codeUrl = requireNonNull(Main.class.getResource("/test.cc"));
		final Path absoluteFile = Paths.get(codeUrl.toURI()).toAbsolutePath().normalize();
		parse(absoluteFile, asList("-std=gnu++20", "-fparse-all-comments"));
	}
}
