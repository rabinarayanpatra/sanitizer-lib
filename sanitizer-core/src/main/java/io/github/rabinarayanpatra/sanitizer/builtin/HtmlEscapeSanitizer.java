package io.github.rabinarayanpatra.sanitizer.builtin;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that escapes basic HTML special characters in rendered output.
 * <p>
 * This sanitizer replaces characters like {@code <}, {@code >}, {@code &},
 * {@code "}, and {@code '} with their corresponding HTML entities. It is
 * intended for simple HTML-escaping in contexts like log output or pre-escaped
 * text rendering.
 * <p>
 * <strong>Security note:</strong> This sanitizer performs basic character
 * escaping only. It does not handle Unicode escapes, null bytes, or
 * double-encoding attacks. It is a formatting utility, not a security control.
 * For XSS prevention, use a dedicated library such as OWASP Java HTML
 * Sanitizer.
 *
 * <pre>
 * String input = "&lt;script&gt;alert('xss')&lt;/script&gt;";
 * String escaped = new HtmlEscapeSanitizer().sanitize(input);
 * // "&amp;lt;script&amp;gt;alert(&amp;#x27;xss&amp;#x27;)&amp;lt;/script&amp;gt;"
 * </pre>
 *
 * @see FieldSanitizer
 * @since 1.0.0
 */
public class HtmlEscapeSanitizer implements FieldSanitizer<String> {

	/**
	 * Default constructor.
	 */
	public HtmlEscapeSanitizer() {
	}

	/**
	 * Escapes HTML special characters in the input string.
	 *
	 * @param in
	 *            the string to sanitize
	 * @return the escaped string, or {@code null} if input is {@code null}
	 */
	@Override
	public @Nullable String sanitize(final @Nullable String in) {
		if (in == null) {
			return null;
		}
		return in.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#x27;");
	}
}
