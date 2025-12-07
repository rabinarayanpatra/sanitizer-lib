package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that trims leading and trailing whitespace from the input string.
 * <p>
 * Commonly used to normalize user input such as names, emails, or free-text
 * fields before storage or validation.
 *
 * <pre>
 * {@code
 * String input = "  example@example.com  ";
 * String sanitized = new TrimSanitizer().sanitize(input); // "example@example.com"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class TrimSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public TrimSanitizer() {
	}

	/**
	 * Trims whitespace from both ends of the input string.
	 *
	 * @param input
	 *              the string to sanitize
	 * @return the trimmed string, or {@code null} if input is {@code null}
	 */
	@Override
	public String sanitize(final String input) {
		if (input == null) {
			return null;
		}

		return input.trim();
	}
}
