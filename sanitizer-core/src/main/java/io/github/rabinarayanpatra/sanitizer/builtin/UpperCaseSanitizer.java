package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
@Component
public class UpperCaseSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    return in == null ? null : in.toUpperCase();
  }
}
