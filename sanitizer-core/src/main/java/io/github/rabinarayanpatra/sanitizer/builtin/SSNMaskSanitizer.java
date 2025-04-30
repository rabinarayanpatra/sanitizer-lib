package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
@Component
public class SSNMaskSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    // strip non-digits
    String d = in.replaceAll("\\D+", "");
    if (d.length() != 9) return in; 
    // format ***-**-1234
    return "***-**-" + d.substring(5);
  }
}
