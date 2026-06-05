package io.github.rabinarayanpatra.sanitizer.core.traversal;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalEngineRecordTest {

	@Test
	void recordRootIsReconstructedWithSanitizedComponent() {
		final Simple input = new Simple("  HELLO  ");
		final Simple out = SanitizationUtils.applyAndReturn(input);
		assertNotSame(input, out);
		assertEquals("hello", out.value());
	}

	@Test
	void recordComponentWithoutAnnotationPassesThrough() {
		final Mixed input = new Mixed("  HELLO  ", 42);
		final Mixed out = SanitizationUtils.applyAndReturn(input);
		assertEquals("hello", out.value());
		assertEquals(42, out.count());
	}

	@Test
	void recordWithNullComponentReconstructed() {
		final Simple input = new Simple(null);
		final Simple out = SanitizationUtils.applyAndReturn(input);
		assertNotSame(input, out);
		assertNull(out.value());
	}

	@Test
	void applyAndReturnNullReturnsNull() {
		assertNull(SanitizationUtils.applyAndReturn(null));
	}

	@Test
	void applyAndReturnPojoReturnsSameRef() {
		final PojoBean b = new PojoBean();
		b.name = "  HELLO  ";
		final PojoBean out = SanitizationUtils.applyAndReturn(b);
		assertEquals(System.identityHashCode(b), System.identityHashCode(out));
		assertEquals("hello", out.name);
	}

	@Test
	void compactConstructorValidationPropagatesAsCause() {
		final Validated input = new Validated("  HELLO  ");
		final RuntimeException ex = assertThrows(RuntimeException.class,
				() -> SanitizationUtils.applyAndReturn(new Validated("   ")));
		assertTrue(ex.getCause() instanceof IllegalArgumentException || ex instanceof IllegalArgumentException
				|| ex.getMessage().contains("must not be blank"));
		assertEquals("  HELLO  ", input.value());
	}

	record Simple(@Sanitize(using = {
			TrimSanitizer.class, LowerCaseSanitizer.class}) String value) {
	}

	record Mixed(@Sanitize(using = {
			TrimSanitizer.class, LowerCaseSanitizer.class}) String value, int count) {
	}

	record Validated(@Sanitize(using = TrimSanitizer.class) String value) {
		public Validated {
			if (value == null || value.isBlank()) {
				throw new IllegalArgumentException("value must not be blank");
			}
		}
	}

	static class PojoBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
	}

	@SuppressWarnings("unused")
	private static final Class<?> KEEP_IMPORT = UpperCaseSanitizer.class;
}
