package io.github.rabinarayanpatra.sanitizer.builtin;

import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

@Component
public class EmailAliasStripSanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    String lower = in.trim().toLowerCase();
    // remove “+alias” before the “@”
    return lower.replaceFirst("\\+[^@]+(?=@)", "");
  }
}
