package com.example;

enum TokenKind {
	Punctuation,

	Keyword,

	Identifier,

	Literal,

	Comment,
	;

	static TokenKind valueOf(final int ordinal) {
		return values()[ordinal];
	}
}
