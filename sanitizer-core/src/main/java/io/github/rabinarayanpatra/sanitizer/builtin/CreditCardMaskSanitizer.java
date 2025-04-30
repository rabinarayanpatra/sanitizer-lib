package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that masks a credit card number by retaining only the last four digits.
 * <p>
 * Non-digit characters are removed before masking. The output format is {@code **** **** **** 1234}, making it safe for
 * logging or display in user interfaces.
 *
 * <pre>
 * {@code
 * String input = "4111-1111-1111-1234";
 * String masked = new CreditCardMaskSanitizer().sanitize(input); // "**** **** **** 1234"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
@Component
public class CreditCardMaskSanitizer implements FieldSanitizer<String> {

  /**
   * Masks all but the last four digits of a credit card number.
   *
   * @param input the credit card number as a string
   * @return the masked credit card string, or {@code "****"} if fewer than four digits remain; returns {@code null} if
   * the input is {@code null}
   */
  @Override
  public String sanitize( final String input ) {
    if( input == null ) {
      return null;
    }

    final String digits = input.replaceAll( "\\D", "" );
    if( digits.length() <= 4 ) {
      return "****";
    }

    final String last4 = digits.substring( digits.length() - 4 );
    return "**** **** **** " + last4;
  }
}
