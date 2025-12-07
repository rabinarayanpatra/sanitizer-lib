package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that trims leading and trailing whitespace and collapses internal
 * whitespace sequences into a single space.
 * <p>
 * Useful for cleaning up user input where excessive spacing may be present,
 * such as names or addresses.
 *
 * <pre>
 * {@code
 * String input = "  John   Doe  ";
 * String sanitized = new CollapseWhitespaceSanitizer().sanitize(input); // "John Doe"
 * }
 * </pre>
 *
 * @see TrimSanitizer
 * @since 1.0.0
 */
public class CollapseWhitespaceSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public CollapseWhitespaceSanitizer() {
	}

	/**
	 * Trims the input and replaces all runs of whitespace with a single space.
	 *
	 * @param in
	 *           the input string to sanitize
	 * @return the sanitized string, or {@code null} if the input was {@code null}
	 */
	@Override
	public String sanitize(final String in) {
		if (in == null) {
			return null;
		}
		return in.trim().replaceAll("\\s+", " ");
	}
}
