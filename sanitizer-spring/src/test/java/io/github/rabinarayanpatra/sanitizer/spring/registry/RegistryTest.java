package io.github.rabinarayanpatra.sanitizer.spring.registry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.github.rabinarayanpatra.sanitizer.builtin.CreditCardMaskSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.spring.registry.RegistryTest.TestConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(classes = TestConfig.class)
class RegistryTest {

	@Autowired
	SanitizerRegistry registry;

	@Test
	void creditCardMaskSanitizerIsAvailable() {
		final var s = registry.get(CreditCardMaskSanitizer.class);
		assertEquals("**** **** **** 7878", s.sanitize("1234456745677878"));
	}

	@Test
	void trimSanitizerIsAvailable() {
		final var s = registry.get(TrimSanitizer.class);
		assertEquals("hello", s.sanitize("  hello  "));
	}

	@Test
	void lowerCaseSanitizerIsAvailable() {
		final var s = registry.get(LowerCaseSanitizer.class);
		assertEquals("hello", s.sanitize("HELLO"));
	}

	@Test
	void upperCaseSanitizerIsAvailable() {
		final var s = registry.get(UpperCaseSanitizer.class);
		assertEquals("HELLO", s.sanitize("hello"));
	}

	@Test
	void throwsForUnregisteredSanitizer() {
		assertThrows(IllegalArgumentException.class, () -> registry.get(UnregisteredSanitizer.class));
	}

	static class UnregisteredSanitizer implements FieldSanitizer<String> {
		@Override
		public String sanitize(final String input) {
			return input;
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@ComponentScan(basePackages = "io.github.rabinarayanpatra.sanitizer")
	static class TestConfig {
	}
}
