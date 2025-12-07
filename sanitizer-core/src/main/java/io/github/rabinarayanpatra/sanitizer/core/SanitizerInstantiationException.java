package io.github.rabinarayanpatra.sanitizer.core;

/**
 * Thrown when a {@link FieldSanitizer} implementation cannot be instantiated.
 * <p>
 * This exception typically occurs when the sanitizer class lacks a no-arg
 * constructor, or the constructor is not accessible at runtime.
 *
 * <pre>
 * {@code
 * throw new SanitizerInstantiationException("Cannot create sanitizer", e);
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public class SanitizerInstantiationException extends RuntimeException {

	/**
	 * Constructs a new {@code SanitizerInstantiationException} with the specified
	 * detail message and cause.
	 *
	 * @param message
	 *            the detail message explaining the failure
	 * @param cause
	 *            the underlying exception that caused instantiation to fail
	 */
	public SanitizerInstantiationException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
