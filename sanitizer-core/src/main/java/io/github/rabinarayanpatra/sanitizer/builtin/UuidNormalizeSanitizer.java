package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class UuidNormalizeSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    String trimmed = in.trim();
    // strip braces
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      trimmed = trimmed.substring(1, trimmed.length() - 1);
    }
    // validate+lowercase
    try {
      UUID uuid = UUID.fromString(trimmed);
      return uuid.toString();
    } catch (IllegalArgumentException e) {
      return trimmed.toLowerCase();
    }
  }
}
