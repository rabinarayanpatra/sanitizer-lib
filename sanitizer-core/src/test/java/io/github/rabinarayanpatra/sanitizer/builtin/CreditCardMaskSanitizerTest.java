package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CreditCardMaskSanitizerTest {

	private final CreditCardMaskSanitizer sanitizer = new CreditCardMaskSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_masksStandardCardNumber() {
		assertEquals("**** **** **** 1234", sanitizer.sanitize("4111111111111234"));
	}

	@Test
	void sanitize_masksCardWithDashes() {
		assertEquals("**** **** **** 1234", sanitizer.sanitize("4111-1111-1111-1234"));
	}

	@Test
	void sanitize_masksCardWithSpaces() {
		assertEquals("**** **** **** 1234", sanitizer.sanitize("4111 1111 1111 1234"));
	}

	@Test
	void sanitize_returnsStarsForFewDigits() {
		assertEquals("****", sanitizer.sanitize("123"));
	}

	@Test
	void sanitize_returnsStarsForExactlyFourDigits() {
		assertEquals("****", sanitizer.sanitize("1234"));
	}
}
