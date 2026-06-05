package io.github.rabinarayanpatra.sanitizer.core;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngine;

/**
 * Public facade for applying {@link Sanitize} annotations to bean fields.
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
		TraversalEngine.walk(bean, TraversalSafetyChecker.ALWAYS);
	}

	/**
	 * Universal sanitization entry point. POJO inputs are mutated in place and the
	 * same reference is returned; record inputs are reconstructed via their
	 * canonical constructor and a new instance is returned.
	 *
	 * @param bean
	 *            the bean to sanitize; may be null
	 * @param <T>
	 *            the static type of the bean
	 * @return the sanitized bean (same ref for POJOs, new instance for records)
	 * @since 1.2.0
	 */
	public static <T> @Nullable T applyAndReturn(final @Nullable T bean) {
		return applyAndReturn(bean, TraversalSafetyChecker.ALWAYS);
	}

	/**
	 * Universal sanitization entry point with a custom safety checker. See
	 * {@link #applyAndReturn(Object)}.
	 *
	 * @param bean
	 *            the bean to sanitize; may be null
	 * @param checker
	 *            the safety checker that gates descent into individual fields; must
	 *            not be null
	 * @param <T>
	 *            the static type of the bean
	 * @return the sanitized bean (same ref for POJOs, new instance for records)
	 * @since 1.2.0
	 */
	@SuppressWarnings("unchecked")
	public static <T> @Nullable T applyAndReturn(final @Nullable T bean, final TraversalSafetyChecker checker) {
		if (bean == null) {
			return null;
		}
		return (T) TraversalEngine.walk(bean, checker);
	}
}
