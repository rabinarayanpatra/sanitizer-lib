package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that masks an International Bank Account Number (IBAN), preserving only the last four characters.
 * <p>
 * All whitespace is removed before masking. The result is a masked string of asterisks followed by the last four
 * characters of the IBAN, helping protect sensitive account details while retaining partial traceability.
 *
 * <pre>
 * {@code
 * String input = "DE89 3704 0044 0532 0130 00";
 * String masked = new IBANMaskSanitizer().sanitize(input);
 * // "********************3000"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
@Component
public class IBANMaskSanitizer implements FieldSanitizer<String> {

  /**
   * Masks all but the last four characters of an IBAN, removing whitespace before processing.
   *
   * @param in the IBAN string to sanitize
   * @return the masked IBAN, or the original input if it has four or fewer characters; {@code null} if input is null
   */
  @Override
  public String sanitize( final String in ) {
    if( in == null ) {
      return null;
    }
    final String plain = in.replaceAll( "\\s+", "" );
    if( plain.length() <= 4 ) {
      return plain;
    }

    final int keep = 4;
    final int len = plain.length();
    return "*".repeat( len - keep ) + plain.substring( len - keep );
  }
}
