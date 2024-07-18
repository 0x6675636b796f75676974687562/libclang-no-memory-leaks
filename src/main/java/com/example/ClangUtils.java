package com.example;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXIndex;
import org.bytedeco.llvm.clang.CXSourceRange;
import org.bytedeco.llvm.clang.CXToken;
import org.bytedeco.llvm.clang.CXTranslationUnit;
import org.bytedeco.llvm.clang.CXUnsavedFile;

import java.nio.file.Path;
import java.util.List;

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
		withPointerScope(new Runnable() {
			@Override
			public void run() {
				withIndex(clang_createIndex(1, 0), new Consumer<CXIndex>() {
					@Override
					public void accept(final CXIndex index) {
						withTranslationUnit(new CXTranslationUnit(), new Consumer<CXTranslationUnit>() {
							@Override
							public void accept(final CXTranslationUnit translationUnit) {
								try (final BytePointer sourceFilename = new BytePointer(absoluteFile.toString())) {
									try (final PointerPointer<Pointer> commandLineArgsPtr = new PointerPointer<>(commandLineArgs.toArray(new String[0]))) {
										try (final CXUnsavedFile unsavedFiles = new CXUnsavedFile()) {
											final int unsavedFilesCount = 0;

											final int errorCode = clang_parseTranslationUnit2(
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
												try (final CXCursor rootCursor = clang_getTranslationUnitCursor(translationUnit)) {
													clang_visitChildren(rootCursor, new AstVisitor(), null);
												}
											} else {
												out.printf("Failed to parse %s; parser returned code %d%n", absoluteFile, errorCode);
											}
										}
									}
								}
							}
						});
					}
				});
			}
		});
	}

	static void withPointerScope(final Runnable block) {
		try (final PointerScope ignored = new PointerScope()) {
			block.run();
		}
	}

	private static void withIndex(
			final CXIndex index,
			final Consumer<CXIndex> block
	) {
		try (final CXIndex ignored = index) {
			try {
				block.accept(index);
			} finally {
				clang_disposeIndex(index);
			}
		}
	}

	private static void withTranslationUnit(
			final CXTranslationUnit translationUnit,
			final Consumer<CXTranslationUnit> block
	) {
		try (final CXTranslationUnit ignored = translationUnit) {
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
		try (final CXSourceRange extent = clang_getCursorExtent(cursor)) {
			try (final CXTranslationUnit translationUnit = clang_Cursor_getTranslationUnit(cursor)) {
				try (final CXToken tokens = new CXToken()) {
					final int[] tokenCountRef = new int[1];
					clang_tokenize(translationUnit, extent, tokens, tokenCountRef);
					final int tokenCount = tokenCountRef[0];
					try {
						for (int index = 0; index < tokenCount; index++) {
							final CXToken token = tokens.position(index);
							action.accept(token);
						}
					} finally {
						tokens.position(0L);
						clang_disposeTokens(translationUnit, tokens, tokenCount);
					}
				}
			}
		}
	}
}
