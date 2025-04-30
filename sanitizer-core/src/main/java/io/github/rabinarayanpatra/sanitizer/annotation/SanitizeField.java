package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
@Repeatable( SanitizeFields.class )
public @interface SanitizeField {
  /**
   * The FieldSanitizer implementation to apply to this field.
   */
  Class<? extends io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer<?>> using();
}
