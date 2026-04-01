package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NullIfBlankSanitizerTest {

	private final NullIfBlankSanitizer sanitizer = new NullIfBlankSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_returnsNullForBlank() {
		assertNull(sanitizer.sanitize("   "));
	}

	@Test
	void sanitize_returnsNullForEmpty() {
		assertNull(sanitizer.sanitize(""));
	}

	@Test
	void sanitize_returnsOriginalForNonBlank() {
		assertEquals("hello", sanitizer.sanitize("hello"));
	}

	@Test
	void sanitize_returnsOriginalWithWhitespaceIfNotBlank() {
		assertEquals("  hello  ", sanitizer.sanitize("  hello  "));
	}
}
