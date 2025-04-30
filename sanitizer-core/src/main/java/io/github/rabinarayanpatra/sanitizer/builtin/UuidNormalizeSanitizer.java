package io.github.rabinarayanpatra.sanitizer.builtin;

import java.util.UUID;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that normalizes a UUID string to lowercase and validates its format.
 * <p>
 * If the input is wrapped in braces (e.g. {@code {uuid}}), they are removed. If the input is a valid UUID, it is
 * normalized to lowercase using {@link UUID#fromString(String)}; otherwise, the lowercased input is returned.
 *
 * <pre>
 * {@code
 * String input = "{A1B2C3D4-E5F6-7A89-0BCD-1234567890EF}";
 * String normalized = new UuidNormalizeSanitizer().sanitize(input); // "a1b2c3d4-e5f6-7a89-0bcd-1234567890ef"
 * }
 * </pre>
 *
 * @see UUID
 * @see FieldSanitizer
 * @since 1.0.0
 */
@Component
public class UuidNormalizeSanitizer implements FieldSanitizer<String> {

  /**
   * Normalizes a UUID string by removing braces, lowercasing, and validating format.
   *
   * @param in the input string to sanitize
   * @return a valid lowercase UUID string, or the lowercased input if parsing fails; {@code null} if input is null
   */
  @Override
  public String sanitize( final String in ) {
    if( in == null ) {
      return null;
    }

    String trimmed = in.trim();

    if( trimmed.startsWith( "{" ) && trimmed.endsWith( "}" ) ) {
      trimmed = trimmed.substring( 1, trimmed.length() - 1 );
    }

    try {
      final UUID uuid = UUID.fromString( trimmed );
      return uuid.toString();
    } catch( final IllegalArgumentException e ) {
      return trimmed.toLowerCase();
    }
  }
}
