package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that trims the input string and returns {@code null} if the result
 * is blank.
 * <p>
 * Useful for converting user input like empty or whitespace-only strings into
 * {@code null}, especially in form handling or persistence layers where
 * {@code null} is preferred over empty strings.
 *
 * <pre>
 * {@code
 * String input = "   ";
 * String result = new NullIfBlankSanitizer().sanitize(input); // null
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class NullIfBlankSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public NullIfBlankSanitizer() {
	}

	/**
	 * Trims the input and returns {@code null} if it is blank.
	 *
	 * @param in
	 *           the string to sanitize
	 * @return {@code null} if input is blank or {@code null}; otherwise, the
	 *         original string
	 */
	@Override
	public String sanitize(final String in) {
		if (in == null) {
			return null;
		}
		return in.trim().isEmpty() ? null : in;
	}
}
