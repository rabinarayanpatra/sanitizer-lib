package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Apply one or more {@link FieldSanitizer} implementations to this field, in
 * the order listed, and/or descend into this field's object graph.
 *
 * <p>
 * This annotation is {@link Repeatable}, so multiple {@code @Sanitize}
 * annotations can be stacked on the same field. Sanitizers are applied in
 * declaration order. When {@link #cascade()} is true on any stacked instance,
 * the engine descends into the field's value after self-sanitizers run.
 *
 * @since 1.0.0
 */
@Repeatable(Sanitizes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sanitize {
	/**
	 * The ordered list of FieldSanitizer implementations to apply to this field's
	 * value. May be empty when the annotation is used solely to opt into
	 * {@link #cascade() cascade}.
	 *
	 * @return the sanitizer classes, possibly empty
	 */
	@SuppressWarnings("java:S1452")
	Class<? extends FieldSanitizer<?>>[] using() default {};

	/**
	 * Optional comma-separated {@code key=value} configuration parameters passed to
	 * {@link io.github.rabinarayanpatra.sanitizer.core.ConfigurableFieldSanitizer}
	 * implementations. Ignored for non-configurable sanitizers.
	 *
	 * <p>
	 * Example: {@code params = "maxLength=100,suffix=..."} or
	 * {@code params = "reveal=4"}
	 *
	 * @return the parameter string, empty by default
	 * @since 1.1.0
	 */
	String params() default "";

	/**
	 * When true, the traversal engine descends into this field's value after any
	 * self-sanitizers run. Supported on fields whose static type is a POJO, record,
	 * {@link java.util.Collection}, or {@link java.util.Map}. Applying
	 * {@code cascade=true} to a leaf type (e.g. {@code String}, primitive, boxed
	 * primitive, {@code UUID}, JSR-310 temporal, {@code BigDecimal},
	 * {@code BigInteger}, {@code Enum}) is rejected at metadata-build time.
	 *
	 * @return true to descend into this field's object graph
	 * @since 1.2.0
	 */
	boolean cascade() default false;
}
