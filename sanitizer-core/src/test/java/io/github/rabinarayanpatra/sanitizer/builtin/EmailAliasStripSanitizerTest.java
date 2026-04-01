package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EmailAliasStripSanitizerTest {

	private final EmailAliasStripSanitizer sanitizer = new EmailAliasStripSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_stripsAlias() {
		assertEquals("john.doe@example.com", sanitizer.sanitize("John.Doe+marketing@example.com"));
	}

	@Test
	void sanitize_lowercasesEmail() {
		assertEquals("user@example.com", sanitizer.sanitize("USER@EXAMPLE.COM"));
	}

	@Test
	void sanitize_noAliasUnchanged() {
		assertEquals("user@example.com", sanitizer.sanitize("user@example.com"));
	}

	@Test
	void sanitize_trimsWhitespace() {
		assertEquals("user@example.com", sanitizer.sanitize("  user@example.com  "));
	}

	@Test
	void sanitize_stripsMultiplePlusSegments() {
		assertEquals("user@example.com", sanitizer.sanitize("user+tag1+tag2@example.com"));
	}
}
