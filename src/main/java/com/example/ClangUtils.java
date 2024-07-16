package com.example;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXToken;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXUnsavedFile;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.lang.System.out;
import static org.bytedeco.llvm.global.clang.CXError_Success;
import static org.bytedeco.llvm.global.clang.CXTranslationUnit_None;
import static org.bytedeco.llvm.global.clang.clang_Cursor_getTranslationUnit;
import static org.bytedeco.llvm.global.clang.clang_createIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeIndex;
import static org.bytedeco.llvm.global.clang.clang_disposeTokens;
import static org.bytedeco.llvm.global.clang.clang_disposeTranslationUnit;
import static org.bytedeco.llvm.global.clang.clang_getCursorExtent;
import static org.bytedeco.llvm.global.clang.clang_getTranslationUnitCursor;
import static org.bytedeco.llvm.global.clang.clang_parseTranslationUnit2;
import static org.bytedeco.llvm.global.clang.clang_tokenize;
import static org.bytedeco.llvm.global.clang.clang_visitChildren;

final class ClangUtils {
	private ClangUtils() {
		assert false;
	}

	static void parse(
			final Path absoluteFile,
			final List<String> commandLineArgs
	) {
		withPointerScope(() -> {
			withIndex(clang_createIndex(1, 0), index -> {
				withTranslationUnit(CXTranslationUnit::new, translationUnit -> {
					try (final var sourceFilename = new BytePointer(absoluteFile.toString())) {
						try (final var commandLineArgsPtr = new PointerPointer<>(commandLineArgs.toArray(new String[0]))) {
							try (final var unsavedFiles = new CXUnsavedFile()) {
								final var unsavedFilesCount = 0;

								final var errorCode = clang_parseTranslationUnit2(
										index,
										sourceFilename,
										commandLineArgsPtr,
										commandLineArgs.size(),
										unsavedFiles,
										unsavedFilesCount,
										CXTranslationUnit_None,
										translationUnit
								);

								if (errorCode == CXError_Success) {
									try (final var rootCursor = clang_getTranslationUnitCursor(translationUnit)) {
										clang_visitChildren(rootCursor, new AstVisitor(), null);
									}
								} else {
									out.printf("Failed to parse %s; parser returned code %d%n", absoluteFile, errorCode);
								}
							}
						}
					}
				});
			});
		});
	}

	static void withPointerScope(final Runnable block) {
		try (final var ignored = new PointerScope()) {
			block.run();
		}
	}

	private static void withIndex(
			final CXIndex index,
			final Consumer<CXIndex> block
	) {
		try (index) {
			try {
				block.accept(index);
			} finally {
				clang_disposeIndex(index);
			}
		}
	}

	private static void withTranslationUnit(
			final Supplier<CXTranslationUnit> lazyTranslationUnit,
			final Consumer<CXTranslationUnit> block
	) {
		try (final var translationUnit = lazyTranslationUnit.get()) {
			try {
				block.accept(translationUnit);
			} finally {
				clang_disposeTranslationUnit(translationUnit);
			}
		}
	}

	static void forEachToken(
			final CXCursor cursor,
			final Consumer<? super CXToken> action
	) {
		try (final var extent = clang_getCursorExtent(cursor)) {
			try (final var translationUnit = clang_Cursor_getTranslationUnit(cursor)) {
				try (final var tokens = new CXToken()) {
					final var tokenCountRef = new int[1];
					clang_tokenize(translationUnit, extent, tokens, tokenCountRef);
					final var tokenCount = tokenCountRef[0];
					try {
						IntStream.range(0, tokenCount)
							 .mapToObj(tokens::position)
							 .forEach(action);
					} finally {
						tokens.position(0L);
						clang_disposeTokens(translationUnit, tokens, tokenCount);
					}
				}
			}
		}
	}
}
