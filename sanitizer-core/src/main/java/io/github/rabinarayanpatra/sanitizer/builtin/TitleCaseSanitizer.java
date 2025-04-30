package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

public class TitleCaseSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize( String input ) {
    if( input == null || input.isBlank() )
      return input;
    String trimmed = input.trim().toLowerCase();
    return Character.toUpperCase( trimmed.charAt( 0 ) ) + trimmed.substring( 1 );
  }
}
