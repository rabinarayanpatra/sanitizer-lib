package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UpperCaseSanitizerTest {

	private final UpperCaseSanitizer sanitizer = new UpperCaseSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_convertsToUpperCase() {
		assertEquals("HELLO WORLD", sanitizer.sanitize("hello world"));
	}

	@Test
	void sanitize_mixedCase() {
		assertEquals("ABC123", sanitizer.sanitize("abc123"));
	}

	@Test
	void sanitize_alreadyUpperCase() {
		assertEquals("HELLO", sanitizer.sanitize("HELLO"));
	}
}
