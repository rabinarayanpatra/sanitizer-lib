package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

public class LowerCaseSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize( String input ) {
    return ( input == null ) ? null : input.toLowerCase();
  }
}
