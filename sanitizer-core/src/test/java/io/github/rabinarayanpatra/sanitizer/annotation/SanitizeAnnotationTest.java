package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanitizeAnnotationTest {

	@Test
	void cascadeDefaultsToFalse() throws NoSuchFieldException {
		final Field field = Fixtures.class.getDeclaredField("trimmedOnly");
		final Sanitize ann = field.getAnnotation(Sanitize.class);
		assertFalse(ann.cascade());
	}

	@Test
	void cascadeReadsTrueWhenSet() throws NoSuchFieldException {
		final Field field = Fixtures.class.getDeclaredField("cascadeOnly");
		final Sanitize ann = field.getAnnotation(Sanitize.class);
		assertTrue(ann.cascade());
		assertArrayEquals(new Class<?>[0], ann.using());
	}

	@Test
	void cascadeCombinedWithUsing() throws NoSuchFieldException {
		final Field field = Fixtures.class.getDeclaredField("trimAndCascade");
		final Sanitize ann = field.getAnnotation(Sanitize.class);
		assertTrue(ann.cascade());
		assertEquals(1, ann.using().length);
		assertEquals(TrimSanitizer.class, ann.using()[0]);
	}

	static class Fixtures {
		@Sanitize(using = TrimSanitizer.class)
		String trimmedOnly;

		@Sanitize(cascade = true)
		Object cascadeOnly;

		@Sanitize(using = TrimSanitizer.class, cascade = true)
		String trimAndCascade;
	}
}
