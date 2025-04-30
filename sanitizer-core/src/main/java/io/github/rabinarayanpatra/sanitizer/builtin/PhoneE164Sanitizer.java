package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
@Component
public class PhoneE164Sanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    // digits only
    String digits = in.replaceAll("\\D+", "");
    // if it already starts with valid country code (e.g. “1” for US), prefix with “+”
    return digits.isEmpty() ? null : "+" + digits;
  }
}
