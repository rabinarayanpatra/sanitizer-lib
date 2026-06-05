package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.lang.reflect.Field;

import jakarta.persistence.PersistenceUnitUtil;

import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * {@link TraversalSafetyChecker} that defers to Hibernate's
 * {@link PersistenceUnitUtil#isLoaded(Object, String)} so the traversal engine
 * skips lazy associations that have not been initialized.
 *
 * @since 1.2.0
 */
public final class HibernateSafetyChecker implements TraversalSafetyChecker {

	private final PersistenceUnitUtil util;

	public HibernateSafetyChecker(final PersistenceUnitUtil util) {
		this.util = util;
	}

	@Override
	public boolean shouldDescend(final Object parent, final Field field) {
		return util.isLoaded(parent, field.getName());
	}
}
