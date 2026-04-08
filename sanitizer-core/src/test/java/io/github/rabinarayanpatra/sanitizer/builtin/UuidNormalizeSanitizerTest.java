package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UuidNormalizeSanitizerTest {

	private final UuidNormalizeSanitizer sanitizer = new UuidNormalizeSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_lowercasesUUID() {
		assertEquals("a1b2c3d4-e5f6-7a89-0bcd-1234567890ef",
				sanitizer.sanitize("A1B2C3D4-E5F6-7A89-0BCD-1234567890EF"));
	}

	@Test
	void sanitize_removesBraces() {
		assertEquals("a1b2c3d4-e5f6-7a89-0bcd-1234567890ef",
				sanitizer.sanitize("{A1B2C3D4-E5F6-7A89-0BCD-1234567890EF}"));
	}

	@Test
	void sanitize_trimsWhitespace() {
		assertEquals("a1b2c3d4-e5f6-7a89-0bcd-1234567890ef",
				sanitizer.sanitize("  A1B2C3D4-E5F6-7A89-0BCD-1234567890EF  "));
	}

	@Test
	void sanitize_invalidUuidFallsBackToLowercase() {
		assertEquals("not-a-uuid", sanitizer.sanitize("NOT-A-UUID"));
	}

	@Test
	void sanitize_leadingBraceOnlyIsNotStripped() {
		// Only one side of the `startsWith("{") && endsWith("}")` branch.
		assertEquals("{abc", sanitizer.sanitize("{ABC"));
	}

	@Test
	void sanitize_trailingBraceOnlyIsNotStripped() {
		assertEquals("abc}", sanitizer.sanitize("ABC}"));
	}
}
