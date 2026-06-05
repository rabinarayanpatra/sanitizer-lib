package io.github.rabinarayanpatra.sanitizer.core;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for sanitizing a single field value.
 *
 * @param <T>
 *            the type of the field (e.g., String, LocalDate, BigDecimal)
 */
@FunctionalInterface
public interface FieldSanitizer<T> {
	/**
	 * Apply sanitization to the input value.
	 *
	 * @param input
	 *            the raw value (maybe null)
	 * @return the sanitized value (maybe null)
	 */
	@Nullable
	T sanitize(@Nullable T input);

	/**
	 * Returns this sanitizer typed as {@code FieldSanitizer<Object>}. The cast is
	 * safe at runtime because the traversal engine routes each sanitizer only to
	 * fields whose declared type matches the sanitizer's generic parameter; a
	 * {@code ClassCastException} thrown by {@code sanitize} is caught and rewrapped
	 * with a descriptive {@code IllegalStateException} by the engine.
	 *
	 * @return this sanitizer as an Object-typed sanitizer
	 * @since 1.2.0
	 */
	@SuppressWarnings("unchecked")
	default FieldSanitizer<Object> asObjectSanitizer() {
		return (FieldSanitizer<Object>) this;
	}
}
