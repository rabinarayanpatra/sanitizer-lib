package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

@Component
public class CollapseWhitespaceSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    // trim, then collapse any run of whitespace into single space
    return in.trim().replaceAll("\\s+", " ");
  }
}
