package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalSafetyCheckerTest {

	@Test
	void alwaysReturnsTrueForAnyParentAndField() throws NoSuchFieldException {
		final Field field = Holder.class.getDeclaredField("name");
		assertTrue(TraversalSafetyChecker.ALWAYS.shouldDescend(new Holder(), field));
	}

	@Test
	void lambdaImplementationIsHonored() throws NoSuchFieldException {
		final Field field = Holder.class.getDeclaredField("name");
		final TraversalSafetyChecker neverDescend = (parent, f) -> false;
		assertFalse(neverDescend.shouldDescend(new Holder(), field));
	}

	static class Holder {
		String name;
	}
}
