package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TitleCaseSanitizerTest {

	private final TitleCaseSanitizer sanitizer = new TitleCaseSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_returnsBlankForBlank() {
		assertEquals("   ", sanitizer.sanitize("   "));
	}

	@Test
	void sanitize_capitalizesEachWord() {
		assertEquals("Hello World", sanitizer.sanitize("heLLo WoRLd"));
	}

	@Test
	void sanitize_trimsWhitespace() {
		assertEquals("Hello World", sanitizer.sanitize("  hello world  "));
	}

	@Test
	void sanitize_singleWord() {
		assertEquals("Java", sanitizer.sanitize("jAVA"));
	}

	@Test
	void sanitize_preservesInternalWhitespace() {
		assertEquals("John  Doe", sanitizer.sanitize("john  doe"));
	}

	@Test
	void sanitize_singleCharacter() {
		assertEquals("A", sanitizer.sanitize("a"));
	}
}
