package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that removes aliasing from email addresses by stripping the {@code +alias} part before the {@code @}.
 * <p>
 * This is commonly used to normalize email addresses for deduplication or identity matching, especially for services
 * like Gmail that allow aliasing via {@code user+label@example.com}.
 *
 * <pre>
 * {@code
 * String input = "John.Doe+marketing@example.com";
 * String sanitized = new EmailAliasStripSanitizer().sanitize(input); // "john.doe@example.com"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
@Component
public class EmailAliasStripSanitizer implements FieldSanitizer<String> {

  /**
   * Normalizes an email address by removing any {@code +alias} from the local part.
   *
   * @param in the input email address
   * @return the normalized email address, or {@code null} if input is {@code null}
   */
  @Override
  public String sanitize( final String in ) {
    if( in == null ) {
      return null;
    }
    final String lower = in.trim().toLowerCase();
    return lower.replaceFirst( "\\+[^@]+(?=@)", "" );
  }
}