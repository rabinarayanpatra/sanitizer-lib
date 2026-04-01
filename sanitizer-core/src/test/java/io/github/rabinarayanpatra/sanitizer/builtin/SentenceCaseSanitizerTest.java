package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SentenceCaseSanitizerTest {

	private final SentenceCaseSanitizer sanitizer = new SentenceCaseSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_returnsBlankForBlank() {
		assertEquals("   ", sanitizer.sanitize("   "));
	}

	@Test
	void sanitize_capitalizesFirstOnly() {
		assertEquals("Hello world", sanitizer.sanitize("heLLo WoRLd"));
	}

	@Test
	void sanitize_trimsWhitespace() {
		assertEquals("Hello world", sanitizer.sanitize("  hello world  "));
	}

	@Test
	void sanitize_singleWord() {
		assertEquals("Java", sanitizer.sanitize("jAVA"));
	}

	@Test
	void sanitize_singleCharacter() {
		assertEquals("A", sanitizer.sanitize("a"));
	}

	@Test
	void sanitize_alreadySentenceCase() {
		assertEquals("Hello world", sanitizer.sanitize("Hello world"));
	}
}
