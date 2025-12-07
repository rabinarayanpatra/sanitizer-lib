package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RemoveNonPrintableSanitizerTest {

	private final RemoveNonPrintableSanitizer sanitizer = new RemoveNonPrintableSanitizer();

	@Test
	void testSanitize_NullInput_ReturnsNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void testSanitize_EmptyString_ReturnsEmptyString() {
		assertEquals("", sanitizer.sanitize(""));
	}

	@Test
	void testSanitize_PrintableCharacters_ReturnsSameString() {
		String input = "Hello World! 123";
		assertEquals(input, sanitizer.sanitize(input));
	}

	@Test
	void testSanitize_ControlCharacters_RemovesControlCharacters() {
		String input = "Hello\u0000\u0001\u0002World\u001F";
		assertEquals("HelloWorld", sanitizer.sanitize(input));
	}

	@Test
	void testSanitize_AllowedWhitespace_PreservesWhitespace() {
		String input = "Line1\nLine2\tTabbed\rReturn";
		assertEquals(input, sanitizer.sanitize(input));
	}

	@Test
	void testSanitize_MixedContent_RemovesOnlyForbidden() {
		String input = "A\u0002B\nC\tD\u001E";
		assertEquals("AB\nC\tD", sanitizer.sanitize(input));
	}
}
