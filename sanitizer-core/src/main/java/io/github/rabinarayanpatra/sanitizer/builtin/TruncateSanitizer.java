package io.github.rabinarayanpatra.sanitizer.builtin;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.core.ConfigurableFieldSanitizer;

/**
 * Configurable sanitizer that truncates strings to a maximum length.
 * <p>
 * Accepts the following parameters via {@code @Sanitize(params = "...")}:
 * <ul>
 * <li>{@code maxLength} — the maximum allowed length (default: 255)</li>
 * <li>{@code suffix} — appended when truncation occurs (default: empty). The
 * suffix length is included in the maxLength limit.</li>
 * </ul>
 *
 * <pre>
 * &#64;Sanitize(using = TruncateSanitizer.class, params = "maxLength=100,suffix=...")
 * private String description;
 * </pre>
 *
 * @see ConfigurableFieldSanitizer
 * @since 1.1.0
 */
public class TruncateSanitizer extends ConfigurableFieldSanitizer<String> {

	private static final int DEFAULT_MAX_LENGTH = 255;

	/**
	 * Default constructor.
	 */
	public TruncateSanitizer() {
	}

	/**
	 * Truncates the input string to the configured maximum length.
	 *
	 * @param input
	 *            the string to sanitize
	 * @return the truncated string, or {@code null} if input is {@code null}
	 */
	@Override
	public @Nullable String sanitize(final @Nullable String input) {
		if (input == null) {
			return null;
		}

		final int maxLength = getIntParam("maxLength", DEFAULT_MAX_LENGTH);
		final String suffix = getParam("suffix", "");

		if (input.length() <= maxLength) {
			return input;
		}

		if (suffix.isEmpty()) {
			return input.substring(0, maxLength);
		}

		final int contentLength = Math.max(0, maxLength - suffix.length());
		return input.substring(0, contentLength) + suffix;
	}
}
