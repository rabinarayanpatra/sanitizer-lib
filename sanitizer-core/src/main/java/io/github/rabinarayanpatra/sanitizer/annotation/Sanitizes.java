package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation for repeatable {@link Sanitize} annotations.
 *
 * <p>
 * This annotation is automatically used by the compiler when multiple
 * {@link Sanitize} annotations are placed on the same field. It should not
 * normally be used directly.
 *
 * @since 1.0.23
 * @see Sanitize
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sanitizes {
	/**
	 * The repeatable {@link Sanitize} annotations.
	 *
	 * @return the contained annotations
	 */
	Sanitize[] value();
}
