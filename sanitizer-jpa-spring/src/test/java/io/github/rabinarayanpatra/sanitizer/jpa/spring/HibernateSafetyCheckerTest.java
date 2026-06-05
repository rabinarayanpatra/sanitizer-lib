package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.lang.reflect.Field;

import jakarta.persistence.PersistenceUnitUtil;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HibernateSafetyCheckerTest {

	@Test
	void shouldDescendReturnsFalseForUnloadedFieldName() throws NoSuchFieldException {
		final PersistenceUnitUtil util = mock(PersistenceUnitUtil.class);
		final Holder holder = new Holder();
		final Field field = Holder.class.getDeclaredField("children");
		when(util.isLoaded(any(), eq("children"))).thenReturn(false);
		final TraversalSafetyChecker checker = new HibernateSafetyChecker(util);
		assertFalse(checker.shouldDescend(holder, field));
	}

	@Test
	void shouldDescendReturnsTrueForLoadedFieldName() throws NoSuchFieldException {
		final PersistenceUnitUtil util = mock(PersistenceUnitUtil.class);
		final Holder holder = new Holder();
		final Field field = Holder.class.getDeclaredField("children");
		when(util.isLoaded(any(), eq("children"))).thenReturn(true);
		final TraversalSafetyChecker checker = new HibernateSafetyChecker(util);
		assertTrue(checker.shouldDescend(holder, field));
	}

	static class Holder {
		java.util.List<String> children;
	}
}
