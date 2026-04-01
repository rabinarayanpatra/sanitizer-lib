package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class IBANMaskSanitizerTest {

	private final IBANMaskSanitizer sanitizer = new IBANMaskSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_masksGermanIBAN() {
		assertEquals("******************3000", sanitizer.sanitize("DE89 3704 0044 0532 0130 00"));
	}

	@Test
	void sanitize_masksIBANWithoutSpaces() {
		assertEquals("******************3000", sanitizer.sanitize("DE89370400440532013000"));
	}

	@Test
	void sanitize_returnsShortInputUnchanged() {
		assertEquals("DE89", sanitizer.sanitize("DE89"));
	}

	@Test
	void sanitize_returnsVeryShortInputUnchanged() {
		assertEquals("AB", sanitizer.sanitize("AB"));
	}
}
