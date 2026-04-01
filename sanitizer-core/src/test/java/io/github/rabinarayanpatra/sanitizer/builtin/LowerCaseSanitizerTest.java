package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LowerCaseSanitizerTest {

	private final LowerCaseSanitizer sanitizer = new LowerCaseSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_convertsToLowerCase() {
		assertEquals("hello world", sanitizer.sanitize("HELLO WORLD"));
	}

	@Test
	void sanitize_mixedCase() {
		assertEquals("helloworld@email.com", sanitizer.sanitize("HelloWorld@Email.Com"));
	}

	@Test
	void sanitize_alreadyLowerCase() {
		assertEquals("hello", sanitizer.sanitize("hello"));
	}
}
