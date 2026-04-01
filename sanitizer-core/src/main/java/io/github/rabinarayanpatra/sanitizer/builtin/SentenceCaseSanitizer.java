package io.github.rabinarayanpatra.sanitizer.builtin;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that converts the input string to sentence case by capitalizing
 * only the first character and lowercasing the rest.
 * <p>
 * Leading and trailing whitespace is trimmed first. If the input is
 * {@code null} or blank, it is returned as-is.
 *
 * <pre>
 * {@code
 * String input = "  heLLo WoRLd ";
 * String sanitized = new SentenceCaseSanitizer().sanitize(input); // "Hello world"
 * }
 * </pre>
 *
 * @see TitleCaseSanitizer
 * @see FieldSanitizer
 * @since 1.1.0
 */
public class SentenceCaseSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public SentenceCaseSanitizer() {
	}

	/**
	 * Converts a trimmed string to sentence case (capitalizes the first letter,
	 * lowercases the rest).
	 *
	 * @param input
	 *            the string to sanitize
	 * @return the sentence-cased string, or the original input if it is
	 *         {@code null} or blank
	 */
	@Override
	public @Nullable String sanitize(final @Nullable String input) {
		if (input == null || input.isBlank()) {
			return input;
		}

		final String trimmed = input.trim().toLowerCase(java.util.Locale.ROOT);
		return Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
	}
}
