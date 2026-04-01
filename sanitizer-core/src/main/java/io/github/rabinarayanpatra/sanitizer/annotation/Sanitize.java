package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Apply one or more {@link FieldSanitizer} implementations to this field, in
 * the order listed. Sanitizers are applied in array order, so
 * {@code @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})}
 * guarantees trim-then-lowercase.
 *
 * <p>
 * This annotation is {@link Repeatable}, so multiple {@code @Sanitize}
 * annotations can be stacked on the same field. Sanitizers are applied in
 * declaration order.
 *
 * <p>
 * Example:
 *
 * <pre>
 * {
 * 	&#64;code
 * 	public class Person {
 * 		&#64;Sanitize(using = TrimSanitizer.class)
 * 		&#64;Sanitize(using = CollapseWhitespaceSanitizer.class)
 * 		&#64;Sanitize(using = LowerCaseSanitizer.class)
 * 		private String fullName;
 * 	}
 * }
 * </pre>
 *
 * @since 1.0.0
 */
@Repeatable(Sanitizes.class)
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
