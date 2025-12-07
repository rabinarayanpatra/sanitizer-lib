package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that converts input text to lowercase using the default locale.
 * <p>
 * Useful for normalization tasks such as preparing emails, usernames, or tags
 * for comparison and storage.
 *
 * <pre>
 * {@code
 * String input = "HelloWorld@Email.Com";
 * String sanitized = new LowerCaseSanitizer().sanitize(input); // "helloworld@email.com"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class LowerCaseSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public LowerCaseSanitizer() {
	}

	/**
	 * Converts the input string to lowercase.
	 *
	 * @param input
	 *              the string to sanitize
	 * @return the lowercase string, or {@code null} if input is {@code null}
	 */
	@Override
	public String sanitize(final String input) {
		return input == null ? null : input.toLowerCase();
	}
}
