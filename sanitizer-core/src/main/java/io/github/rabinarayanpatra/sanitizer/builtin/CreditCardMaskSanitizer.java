package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Masks a credit‐card number by keeping only the last 4 digits; strips non‐digits first, then formats as "**** ****
 * **** 1234".
 */
@Component
public class CreditCardMaskSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize( String input ) {
    if( input == null )
      return null;
    // remove all non-digit characters
    String digits = input.replaceAll( "\\D", "" );
    if( digits.length() <= 4 ) {
      // too short to mask meaningfully
      return "****";
    }
    // keep last 4 digits
    String last4 = digits.substring( digits.length() - 4 );
    return "**** **** **** " + last4;
  }
}
