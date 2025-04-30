package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that replaces characters not allowed in most filenames with underscores.
 * <p>
 * This includes characters like {@code \ / : * ? " < > |}, which are restricted on Windows and other common
 * filesystems. Useful for safely storing user-generated filenames or exporting reports.
 *
 * <pre>
 * {@code
 * String input = "user:data/report|2025?.pdf";
 * String safe = new SafeFilenameSanitizer().sanitize(input); // "user_data_report_2025_.pdf"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
@Component
public class SafeFilenameSanitizer implements FieldSanitizer<String> {

  /**
   * Replaces filesystem-reserved characters with underscores in the input string.
   *
   * @param in the proposed filename string
   * @return a sanitized filename-safe string, or {@code null} if input is {@code null}
   */
  @Override
  public String sanitize( final String in ) {
    if( in == null ) {
      return null;
    }
    return in.trim().replaceAll( "[\\\\/:*?\"<>|]", "_" );
  }
}

