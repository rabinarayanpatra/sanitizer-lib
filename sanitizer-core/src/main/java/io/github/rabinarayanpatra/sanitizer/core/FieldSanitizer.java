package io.github.rabinarayanpatra.sanitizer.core;

/**
 * Strategy interface for sanitizing a single field value.
 *
 * @param <T> the type of the field (e.g., String, LocalDate, BigDecimal)
 */
public interface FieldSanitizer<T> {
  /**
   * Apply sanitization to the input value.
   *
   * @param input the raw value (maybe null)
   * @return the sanitized value (maybe null)
   */
  T sanitize( T input );
}
