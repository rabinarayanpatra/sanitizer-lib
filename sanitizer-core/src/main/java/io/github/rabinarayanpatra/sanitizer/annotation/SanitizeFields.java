package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeating {@link SanitizeField} annotations on a single field.
 * <p>
 * This is automatically used when multiple {@code @SanitizeField} annotations are applied to a field, and does not need
 * to be declared manually.
 *
 * <pre>
 * {@code
 * public class Person {
 *   @SanitizeField(using = TrimSanitizer.class)
 *   @SanitizeField(using = CollapseWhitespaceSanitizer.class)
 *   private String fullName;
 * }
 * }
 * </pre>
 *
 * @return an array of {@link SanitizeField} annotations to apply
 * @since 1.0.0
 */
@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.FIELD )
public @interface SanitizeFields {
  /**
   * The array of {@link SanitizeField} annotations applied to the field.
   *
   * @return the sanitizers to apply in order
   */
  SanitizeField[] value();
}
