package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import java.text.Normalizer;

import org.springframework.stereotype.Component;

@Component
public class SlugifySanitizer implements FieldSanitizer<String> {
  @Override
  public String sanitize(String in) {
    if (in == null) return null;
    String noAccent = Normalizer.normalize(in, Normalizer.Form.NFD)
                             .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    // lowercase, keep letters/numbers, replace runs of non-alnum with single dash
    return noAccent
      .toLowerCase()
      .replaceAll("[^a-z0-9]+", "-")
      .replaceAll("(^-|-$)", "");
  }
}
