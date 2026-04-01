package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SlugifySanitizerTest {

	private final SlugifySanitizer sanitizer = new SlugifySanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_basicSlugification() {
		assertEquals("cafe-con-leches", sanitizer.sanitize("Café Con Leches!"));
	}

	@Test
	void sanitize_stripsDiacritics() {
		assertEquals("uber", sanitizer.sanitize("Über"));
	}

	@Test
	void sanitize_replacesSpecialChars() {
		assertEquals("hello-world", sanitizer.sanitize("Hello, World!"));
	}

	@Test
	void sanitize_removesLeadingTrailingDashes() {
		assertEquals("hello", sanitizer.sanitize("--hello--"));
	}

	@Test
	void sanitize_lowercases() {
		assertEquals("hello-world", sanitizer.sanitize("HELLO WORLD"));
	}
}
