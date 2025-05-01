package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that converts the input string to a title case by capitalizing the first character and lowercasing the
 * rest.
 * <p>
 * Leading and trailing whitespace is trimmed first. If the input is {@code null} or blank, it is returned as-is.
 *
 * <pre>
 * {@code
 * String input = "  heLLo WoRLd ";
 * String sanitized = new TitleCaseSanitizer().sanitize(input); // "Hello world"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class TitleCaseSanitizer implements FieldSanitizer<String> {

  /**
   * Converts a trimmed string to a title case (capitalizes first letter, lowercases the rest).
   *
   * @param input the string to sanitize
   * @return the title-cased string, or the original input if it is {@code null} or blank
   */
  @Override
  public String sanitize( final String input ) {
    if( input == null || input.isBlank() ) {
      return input;
    }

    final String trimmed = input.trim().toLowerCase();
    return Character.toUpperCase( trimmed.charAt( 0 ) ) + trimmed.substring( 1 );
  }
}
