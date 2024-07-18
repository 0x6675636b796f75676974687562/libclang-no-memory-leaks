package com.example;

import org.bytedeco.llvm.clang.CXClientData;
import org.bytedeco.llvm.clang.CXCursor;
import org.bytedeco.llvm.clang.CXCursorVisitor;
import org.bytedeco.llvm.clang.CXString;
import org.bytedeco.llvm.clang.CXToken;
import org.bytedeco.llvm.clang.CXTranslationUnit;

import static com.example.ClangUtils.forEachToken;
import static com.example.ClangUtils.withPointerScope;
import static java.lang.System.out;
import static org.bytedeco.llvm.global.clang.CXChildVisit_Recurse;
import static org.bytedeco.llvm.global.clang.clang_Cursor_getTranslationUnit;
import static org.bytedeco.llvm.global.clang.clang_getCursorKind;
import static org.bytedeco.llvm.global.clang.clang_getCursorKindSpelling;
import static org.bytedeco.llvm.global.clang.clang_getTokenKind;
import static org.bytedeco.llvm.global.clang.clang_getTokenSpelling;

final class AstVisitor extends CXCursorVisitor {
	@Override
	public int call(final CXCursor cursor, final CXCursor parent, final CXClientData clientData) {
		try (final CXCursor c = cursor; final CXCursor p = parent; final CXClientData d = clientData) {
			/*-
			 * Entering a new `PointerScope` here is 100% necessary,
			 * probably because the outer ("lower") stack frame is a
			 * native one (i.e. `call()` is directly invoked by the
			 * native code).
			 *
			 * Despite previously registered ("outer") pointer scopes
			 * are still visible, having only a single scope per
			 * translation unit (i.e., AST tree) rather than per cursor
			 * eventually results in 100% usage of all CPU cores -- in
			 * the native code.
			 */
			withPointerScope(new Runnable() {
				@Override
				public void run() {
					try (final CXString spelling = clang_getCursorKindSpelling(clang_getCursorKind(cursor))) {
						out.println(spelling.getString());
					}

					try (final CXTranslationUnit translationUnit = clang_Cursor_getTranslationUnit(cursor)) {
						forEachToken(cursor, new Consumer<CXToken>() {
							@Override
							public void accept(final CXToken token) {
								final TokenKind kind = TokenKind.valueOf(clang_getTokenKind(token));
								try (final CXString spelling = clang_getTokenSpelling(translationUnit, token)) {
									out.printf("\t%s(\"%s\")%n", kind, spelling.getString());
								}
							}
						});
					}
				}
			});
		}

		return CXChildVisit_Recurse;
	}
}
