package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that converts the input string to title case by capitalizing the
 * first character of each word and lowercasing the rest.
 * <p>
 * Leading and trailing whitespace is trimmed first. If the input is
 * {@code null} or blank, it is returned as-is.
 *
 * <pre>
 * {@code
 * String input = "  heLLo WoRLd ";
 * String sanitized = new TitleCaseSanitizer().sanitize(input); // "Hello World"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class TitleCaseSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public TitleCaseSanitizer() {
	}

	/**
	 * Converts a trimmed string to title case (capitalizes the first letter of each
	 * word, lowercases the rest).
	 *
	 * @param input
	 *            the string to sanitize
	 * @return the title-cased string, or the original input if it is {@code null}
	 *         or blank
	 */
	@Override
	public String sanitize(final String input) {
		if (input == null || input.isBlank()) {
			return input;
		}

		final String trimmed = input.trim();
		final StringBuilder sb = new StringBuilder(trimmed.length());
		boolean capitalizeNext = true;

		for (int i = 0; i < trimmed.length(); i++) {
			final char c = trimmed.charAt(i);
			if (Character.isWhitespace(c)) {
				sb.append(c);
				capitalizeNext = true;
			} else if (capitalizeNext) {
				sb.append(Character.toUpperCase(c));
				capitalizeNext = false;
			} else {
				sb.append(Character.toLowerCase(c));
			}
		}

		return sb.toString();
	}
}
