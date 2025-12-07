package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that normalizes a phone number to E.164 format by stripping
 * non-digit characters and prepending a {@code +}.
 * <p>
 * This sanitizer assumes the input already includes the full international
 * number with country code. It is useful for ensuring consistent phone number
 * representation across systems.
 *
 * <pre>
 * {@code
 * String input = "(202) 555-0198";
 * String sanitized = new PhoneE164Sanitizer().sanitize(input); // "+2025550198"
 * }
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class PhoneE164Sanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public PhoneE164Sanitizer() {
	}

	/**
	 * Converts the input to E.164 format by removing all non-digit characters and
	 * prefixing with {@code +}.
	 *
	 * @param in
	 *           the input phone number
	 * @return the normalized E.164 phone number, or {@code null} if the result is
	 *         empty or input is {@code null}
	 */
	@Override
	public String sanitize(final String in) {
		if (in == null) {
			return null;
		}
		final String digits = in.replaceAll("\\D+", "");
		return digits.isEmpty() ? null : "+" + digits;
	}
}
