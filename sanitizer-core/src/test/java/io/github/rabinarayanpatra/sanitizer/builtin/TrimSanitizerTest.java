package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TrimSanitizerTest {

	private final TrimSanitizer sanitizer = new TrimSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_trimsLeadingAndTrailingWhitespace() {
		assertEquals("hello", sanitizer.sanitize("  hello  "));
	}

	@Test
	void sanitize_preservesInternalWhitespace() {
		assertEquals("hello world", sanitizer.sanitize("  hello world  "));
	}

	@Test
	void sanitize_returnsEmptyForWhitespaceOnly() {
		assertEquals("", sanitizer.sanitize("   "));
	}

	@Test
	void sanitize_returnsUnchangedIfAlreadyTrimmed() {
		assertEquals("hello", sanitizer.sanitize("hello"));
	}
}
