package io.github.rabinarayanpatra.sanitizer.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * JPA entity listener that applies sanitizers before persist and before update.
 * Wire it up by annotating an entity (or a {@code @MappedSuperclass}) with
 * {@code @EntityListeners(SanitizationEntityListener.class)}.
 *
 * <p>
 * In a Spring + Hibernate context, the listener is instantiated by Hibernate.
 * Use the {@code sanitizer-jpa-spring} starter to wire a Hibernate-aware
 * {@link TraversalSafetyChecker} that skips lazy associations.
 *
 * @since 1.0.0
 */
public class SanitizationEntityListener {

	private TraversalSafetyChecker safetyChecker = TraversalSafetyChecker.ALWAYS;

	/**
	 * Overrides the default {@link TraversalSafetyChecker#ALWAYS} checker. Spring
	 * auto-configuration in {@code sanitizer-jpa-spring} calls this with a
	 * Hibernate-aware implementation.
	 *
	 * @param safetyChecker
	 *            the checker to use for all subsequent {@link #onSave(Object)}
	 *            invocations; must not be null
	 */
	public void setSafetyChecker(final TraversalSafetyChecker safetyChecker) {
		this.safetyChecker = safetyChecker;
	}

	@PrePersist
	@PreUpdate
	public void onSave(final Object entity) {
		SanitizationUtils.applyAndReturn(entity, safetyChecker);
	}
}
