package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Apply one or more {@link FieldSanitizer} implementations to this field, in
 * the order listed.
 *
 * <p>
 * Example:
 *
 * <pre>{@code
 * public class Person {
 * 	@Sanitize(using = {TrimSanitizer.class, CollapseWhitespaceSanitizer.class, LowerCaseSanitizer.class})
 * 	private String fullName;
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sanitize {
	/**
	 * The ordered list of FieldSanitizer implementations to apply.
	 *
	 * @return the sanitizer classes
	 */
	@SuppressWarnings("java:S1452")
	Class<? extends FieldSanitizer<?>>[] using();
}
