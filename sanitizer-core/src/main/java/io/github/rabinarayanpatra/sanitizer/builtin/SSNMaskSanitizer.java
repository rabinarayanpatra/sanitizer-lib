package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that masks a U.S. Social Security Number (SSN), revealing only the
 * last four digits.
 * <p>
 * Non-digit characters are removed before masking. If the cleaned input does
 * not contain exactly 9 digits, the original input is returned unchanged.
 *
 * <pre>
 * {@code
 * String input = "123-45-6789";
 * String masked = new SSNMaskSanitizer().sanitize(input); // "***-**-6789"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class SSNMaskSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public SSNMaskSanitizer() {
	}

	/**
	 * Masks a Social Security Number by stripping non-digit characters and
	 * preserving only the last four digits.
	 *
	 * @param in
	 *            the input SSN string
	 * @return the masked SSN, or the original input if the cleaned string is not 9
	 *         digits; {@code null} if input is null
	 */
	@Override
	public String sanitize(final String in) {
		if (in == null) {
			return null;
		}

		final String digits = in.replaceAll("\\D+", "");
		if (digits.length() != 9) {
			return in;
		}

		return "***-**-" + digits.substring(5);
	}
}
