package io.github.rabinarayanpatra.sanitizer.core;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngine;
import io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalState;

/**
 * Public facade for applying {@link Sanitize} annotations to bean fields. All
 * traversal logic lives in
 * {@link io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngine};
 * this class is a thin entry point.
 *
 * @since 1.0.0
 */
public final class SanitizationUtils {

	private SanitizationUtils() {
	}

	/**
	 * Applies sanitizers in place to a POJO. Throws for record inputs because
	 * record components cannot be mutated and the void return would discard the
	 * reconstructed instance. Use {@link #applyAndReturn(Object)} for records.
	 *
	 * @param bean
	 *            the POJO whose fields should be sanitized; may be null
	 * @throws IllegalArgumentException
	 *             when {@code bean} is a {@link Record} instance
	 */
	public static void apply(final @Nullable Object bean) {
		if (bean == null) {
			return;
		}
		if (bean.getClass().isRecord()) {
			throw new IllegalArgumentException("apply(Object) discards return value but record requires reassignment. "
					+ "Use applyAndReturn(T) for record type " + bean.getClass().getName() + ".");
		}
		TraversalEngine.walk(bean, new TraversalState(), TraversalSafetyChecker.ALWAYS);
	}
}
