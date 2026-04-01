package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SSNMaskSanitizerTest {

	private final SSNMaskSanitizer sanitizer = new SSNMaskSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_masksFormattedSSN() {
		assertEquals("***-**-6789", sanitizer.sanitize("123-45-6789"));
	}

	@Test
	void sanitize_masksUnformattedSSN() {
		assertEquals("***-**-6789", sanitizer.sanitize("123456789"));
	}

	@Test
	void sanitize_returnsOriginalIfNot9Digits() {
		assertEquals("1234", sanitizer.sanitize("1234"));
	}

	@Test
	void sanitize_returnsOriginalIfTooManyDigits() {
		assertEquals("1234567890", sanitizer.sanitize("1234567890"));
	}
}
