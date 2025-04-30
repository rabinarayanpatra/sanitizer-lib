package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
@Component
public class SafeFilenameSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    // strip characters not allowed on most filesystems
    return in.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
