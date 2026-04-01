package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CollapseWhitespaceSanitizerTest {

	private final CollapseWhitespaceSanitizer sanitizer = new CollapseWhitespaceSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_collapsesInternalWhitespace() {
		assertEquals("John Doe", sanitizer.sanitize("John   Doe"));
	}

	@Test
	void sanitize_trimsAndCollapses() {
		assertEquals("John Doe", sanitizer.sanitize("  John   Doe  "));
	}

	@Test
	void sanitize_singleSpaceUnchanged() {
		assertEquals("a b", sanitizer.sanitize("a b"));
	}

	@Test
	void sanitize_tabsAndNewlines() {
		assertEquals("a b c", sanitizer.sanitize("a\t\tb\n\nc"));
	}
}
