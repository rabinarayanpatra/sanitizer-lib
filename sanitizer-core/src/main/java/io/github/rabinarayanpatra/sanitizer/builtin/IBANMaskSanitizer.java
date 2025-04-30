package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
@Component
public class IBANMaskSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    String plain = in.replaceAll("\\s+", "");
    if (plain.length() <= 4) return plain;
    // mask every character except last 4
    int keep = 4, len = plain.length();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len - keep; i++) sb.append('*');
    sb.append(plain.substring(len - keep));
    return sb.toString();
  }
}
