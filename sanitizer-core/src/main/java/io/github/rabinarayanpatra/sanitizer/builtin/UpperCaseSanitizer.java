package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that converts the input string to uppercase using the default locale.
 * <p>
 * Useful for normalizing values like country codes, acronyms, or identifiers for consistent comparison or storage.
 *
 * <pre>
 * {@code
 * String input = "abc123";
 * String sanitized = new UpperCaseSanitizer().sanitize(input); // "ABC123"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class UpperCaseSanitizer implements FieldSanitizer<String> {

  /**
   * Converts the input string to uppercase.
   *
   * @param in the string to sanitize
   * @return the uppercase version of the input, or {@code null} if input is {@code null}
   */
  @Override
  public String sanitize( final String in ) {
    if( in == null ) {
      return null;
    }

    return in.toUpperCase();
  }
}

