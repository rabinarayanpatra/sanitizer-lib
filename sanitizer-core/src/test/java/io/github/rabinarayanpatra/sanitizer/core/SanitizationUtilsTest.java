package io.github.rabinarayanpatra.sanitizer.core;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SanitizationUtilsTest {

	@Test
	void apply_sanitizesAnnotatedFields() {
		final MutableBean bean = new MutableBean();
		bean.name = "  HELLO  ";
		SanitizationUtils.apply(bean);
		assertEquals("hello", bean.name);
	}

	@Test
	void apply_skipsNullBean() {
		// Should not throw
		SanitizationUtils.apply(null);
	}

	@Test
	void apply_skipsNullFieldValue() {
		final MutableBean bean = new MutableBean();
		bean.name = null;
		// Should not throw — sanitizers handle null internally
		SanitizationUtils.apply(bean);
	}

	@Test
	void apply_throwsOnInaccessibleField() {
		final ImmutableRecord rec = new ImmutableRecord("  HELLO  ");
		final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> SanitizationUtils.apply(rec));
		// Verify the exception message contains useful diagnostic info
		assertEquals(true, ex.getMessage().contains("name"));
		assertEquals(true, ex.getMessage().contains("ImmutableRecord"));
		assertEquals(true, ex.getCause() instanceof IllegalAccessException);
	}

	// --- Test fixtures ---

	static class MutableBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
	}

	record ImmutableRecord(@Sanitize(using = TrimSanitizer.class) String name) {
	}
}
