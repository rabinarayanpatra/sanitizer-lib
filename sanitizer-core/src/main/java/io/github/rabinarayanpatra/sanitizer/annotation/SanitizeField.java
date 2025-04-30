package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Indicates that a field should be sanitized using the specified {@link FieldSanitizer} implementation.
 * <p>
 * This annotation can be applied to any field to enforce sanitization either at runtime (via reflection) or
 * during data binding, persistence, or other integration hooks depending on the active Sanitizer module
 * (e.g., Spring or JPA).
 * <p>
 * Multiple {@code @SanitizeField} annotations may be applied to a single field by using {@link Repeatable}.
 *
 * <pre>
 * {@code
 * public class UserInput {
 *   @SanitizeField(using = TrimSanitizer.class)
 *   @SanitizeField(using = CollapseWhitespaceSanitizer.class)
 *   private String name;
 * }
 * }
 * </pre>
 *
 * @return the {@link FieldSanitizer} class to use for this field
 * @since 1.0.0
 */

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
@Repeatable( SanitizeFields.class )
public @interface SanitizeField {

  /**
   * Specifies the {@link FieldSanitizer} implementation to apply to this field.
   *
   * @return the class of the {@code FieldSanitizer} to use
   */
  @SuppressWarnings("java:S1452")
  Class<? extends FieldSanitizer<?>> using();
}
