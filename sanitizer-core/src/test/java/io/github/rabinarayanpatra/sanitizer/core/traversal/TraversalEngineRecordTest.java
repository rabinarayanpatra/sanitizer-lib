package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	@Test
	void canonicalCtorRuntimeExceptionAfterSanitizationIsRethrown() {
		// Sanitizer turns input into null; compact ctor rejects null. This
		// exercises the InvocationTargetException catch on the canonical
		// constructor call and the RuntimeException branch of rethrowRuntime.
		final RuntimeException ex = assertThrows(RuntimeException.class,
				() -> SanitizationUtils.applyAndReturn(new NullRejecting("blank-makes-me-null")));
		assertTrue(ex instanceof IllegalStateException || ex.getMessage().contains("rejected null"));
	}

	@Test
	void canonicalCtorErrorWrappedAsIllegalStateException() {
		// Throw a non-RuntimeException Throwable (Error) from compact ctor body
		// only on reconstruction, so we can build the original then trigger the
		// fallback branch in rethrowRuntime where the cause is not a
		// RuntimeException.
		final ErrorThrower input = new ErrorThrower("anything");
		ErrorThrower.failNext = true;
		try {
			final IllegalStateException ex = assertThrows(IllegalStateException.class,
					() -> SanitizationUtils.applyAndReturn(input));
			assertTrue(ex.getMessage().contains("Record canonical constructor"));
			assertTrue(ex.getCause() instanceof Error);
		} finally {
			ErrorThrower.failNext = false;
		}
	}

	@Test
	void recordWithListCollectionCascadesIntoElements() {
		// Exercises walkRecord's COLLECTION/MAP branch (lines 64-71): the chain
		// applies per-element and the descended collection is placed into the
		// reconstructed record.
		final WithList input = new WithList(new ArrayList<>(List.of("  A  ", "  B  ")));
		final WithList out = SanitizationUtils.applyAndReturn(input);
		assertEquals(List.of("a", "b"), out.values());
	}

	@Test
	void recordWithMapCollectionCascadesIntoValues() {
		final LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put("first", "  A  ");
		map.put("second", "  B  ");
		final WithMap input = new WithMap(map);
		final WithMap out = SanitizationUtils.applyAndReturn(input);
		assertEquals("a", out.byKey().get("first"));
		assertEquals("b", out.byKey().get("second"));
	}

	@Test
	void recordWithNullCollectionComponentLeftAsNull() {
		// Triggers the (raw != null) false branch of the conditional in the
		// COLLECTION/MAP branch of walkRecord.
		final WithList out = SanitizationUtils.applyAndReturn(new WithList(null));
		assertNull(out.values());
	}

	@Test
	void recordChainTypeMismatchProducesIllegalState() {
		// Trim sanitizer (FieldSanitizer<String>) applied to Integer component:
		// triggers ClassCastException in applyChain, caught at walkRecord and
		// rewrapped as IllegalStateException with the component name.
		final IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> SanitizationUtils.applyAndReturn(new MismatchedRecord(42)));
		assertTrue(ex.getMessage().contains("Type mismatch"));
		assertTrue(ex.getMessage().contains("count"));
		assertTrue(ex.getCause() instanceof ClassCastException);
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

	public static class NullingSanitizer implements io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer<String> {
		public NullingSanitizer() {
		}

		@Override
		public @org.jspecify.annotations.Nullable String sanitize(
				final @org.jspecify.annotations.Nullable String input) {
			return null;
		}
	}

	record NullRejecting(@Sanitize(using = NullingSanitizer.class) String value) {
		public NullRejecting {
			if (value == null) {
				throw new IllegalStateException("rejected null in canonical ctor");
			}
		}
	}

	record ErrorThrower(@Sanitize(using = TrimSanitizer.class) String value) {
		static boolean failNext;
		public ErrorThrower {
			if (failNext) {
				throw new java.io.IOError(new RuntimeException("boom from ctor"));
			}
		}
	}

	record WithList(@Sanitize(using = {
			TrimSanitizer.class, LowerCaseSanitizer.class}) List<String> values) {
	}

	record WithMap(@Sanitize(using = {
			TrimSanitizer.class, LowerCaseSanitizer.class}) Map<String, String> byKey) {
	}

	record MismatchedRecord(@Sanitize(using = TrimSanitizer.class) Integer count) {
	}

	static class PojoBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
	}

	@SuppressWarnings("unused")
	private static final Class<?> KEEP_IMPORT = UpperCaseSanitizer.class;
}
