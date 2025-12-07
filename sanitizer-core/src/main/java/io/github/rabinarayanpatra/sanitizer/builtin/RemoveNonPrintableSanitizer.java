package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that removes non-printable control characters from the input
 * string.
 * <p>
 * This sanitizer removes all characters in the Unicode "Control" category (Cc,
 * Cf, Cs, Co, Cn), except for the common whitespace characters: Tab (\t), Line
 * Feed (\n), and Carriage Return (\r).
 * </p>
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 * String input = "Hello\u0002World";
 * String sanitized = new RemoveNonPrintableSanitizer().sanitize(input); // "HelloWorld"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class RemoveNonPrintableSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public RemoveNonPrintableSanitizer() {
	}

	/**
	 * Removes non-printable characters from the input string.
	 *
	 * @param input
	 *            the string to sanitize
	 * @return the sanitized string, or {@code null} if input is {@code null}
	 */
	@Override
	public String sanitize(final String input) {
		if (input == null) {
			return null;
		}

		// Regex to match control characters but exclude tab, newline, and carriage
		// return
		return input.replaceAll("[\\p{Cntrl}&&[^\\t\\r\\n]]", "");
	}
}
