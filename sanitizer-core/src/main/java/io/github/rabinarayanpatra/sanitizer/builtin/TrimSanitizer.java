package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

@Component
public class TrimSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize( String input ) {
    return ( input == null ) ? null : input.trim();
  }
}
