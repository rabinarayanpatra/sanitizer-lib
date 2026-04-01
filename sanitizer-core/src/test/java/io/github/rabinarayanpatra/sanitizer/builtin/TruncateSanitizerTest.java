package io.github.rabinarayanpatra.sanitizer.builtin;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TruncateSanitizerTest {

	@Test
	void sanitize_returnsNullForNull() {
		final TruncateSanitizer sanitizer = new TruncateSanitizer();
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_defaultMaxLengthIs255() {
		final TruncateSanitizer sanitizer = new TruncateSanitizer();
		final String input = "a".repeat(300);
		assertEquals(255, sanitizer.sanitize(input).length());
	}

	@Test
	void sanitize_respectsConfiguredMaxLength() {
		final TruncateSanitizer sanitizer = new TruncateSanitizer();
		sanitizer.configure(Map.of("maxLength", "10"));
		assertEquals("HelloWorld", sanitizer.sanitize("HelloWorldExtra"));
	}

	@Test
	void sanitize_appendsSuffix() {
		final TruncateSanitizer sanitizer = new TruncateSanitizer();
		sanitizer.configure(Map.of("maxLength", "10", "suffix", "..."));
		assertEquals("HelloWo...", sanitizer.sanitize("HelloWorldExtra"));
	}

	@Test
	void sanitize_noTruncationWhenWithinLimit() {
		final TruncateSanitizer sanitizer = new TruncateSanitizer();
		sanitizer.configure(Map.of("maxLength", "20"));
		assertEquals("short", sanitizer.sanitize("short"));
	}

	@Test
	void sanitize_worksViaAnnotation() {
		final TruncateBean bean = new TruncateBean();
		bean.name = "a".repeat(50);
		SanitizationUtils.apply(bean);
		assertEquals(20, bean.name.length());
	}

	@Test
	void sanitize_worksViaAnnotationWithSuffix() {
		final TruncateSuffixBean bean = new TruncateSuffixBean();
		bean.name = "a".repeat(50);
		SanitizationUtils.apply(bean);
		assertEquals(20, bean.name.length());
		assertEquals("...", bean.name.substring(17));
	}

	static class TruncateBean {
		@Sanitize(using = TruncateSanitizer.class, params = "maxLength=20")
		String name;
	}

	static class TruncateSuffixBean {
		@Sanitize(using = TruncateSanitizer.class, params = "maxLength=20,suffix=...")
		String name;
	}
}
