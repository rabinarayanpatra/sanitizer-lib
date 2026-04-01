package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PhoneE164SanitizerTest {

	private final PhoneE164Sanitizer sanitizer = new PhoneE164Sanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_formatsUSPhone() {
		assertEquals("+2025550198", sanitizer.sanitize("(202) 555-0198"));
	}

	@Test
	void sanitize_stripsAllNonDigits() {
		assertEquals("+12345678", sanitizer.sanitize("+1-234-567-8"));
	}

	@Test
	void sanitize_returnsNullForNoDigits() {
		assertNull(sanitizer.sanitize("no-digits-here!"));
	}

	@Test
	void sanitize_plainDigits() {
		assertEquals("+1234567890", sanitizer.sanitize("1234567890"));
	}
}
