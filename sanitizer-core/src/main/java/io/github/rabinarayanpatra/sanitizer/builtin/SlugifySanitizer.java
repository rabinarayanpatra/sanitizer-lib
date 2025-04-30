package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import org.springframework.stereotype.Component;

/**
 * Sanitizer that converts a string into a lowercase, URL-friendly "slug" format.
 * <p>
 * Diacritics and accents are stripped, non-alphanumeric characters are replaced with dashes, and
 * leading/trailing dashes are removed. Commonly used for generating safe identifiers or SEO-friendly URLs.
 *
 * <pre>
 * {@code
 * String input = "Café Con Leches!";
 * String slug = new SlugifySanitizer().sanitize(input); // "cafe-con-leches"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
@Component
public class SlugifySanitizer implements FieldSanitizer<String> {

  /**
   * Normalizes a string to a slug by removing diacritics, lowercasing, and replacing non-alphanumerics with dashes.
   *
   * @param in the input string to slugify
   * @return the slugified string, or {@code null} if input is {@code null}
   */
  @Override
  public String sanitize( final String in) {
    if (in == null) {
      return null;
    }
    final String noAccent = Normalizer.normalize(in, Form.NFD)
        .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    return noAccent
        .toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-+)|(-+$)", "");
  }
}
