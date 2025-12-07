package io.github.rabinarayanpatra.sanitizer.builtin;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Sanitizer that escapes basic HTML special characters to prevent injection in
 * rendered output.
 * <p>
 * This sanitizer replaces characters like {@code <}, {@code >}, {@code &},
 * {@code "}, and {@code '} with their corresponding HTML entities. It is
 * intended for simple HTML-escaping in contexts like log output or pre-escaped
 * text rendering.
 *
 * <pre>
 * {@code
 * String input = "<script>alert('xss')</script>";
 * String escaped = new HtmlEscapeSanitizer().sanitize(input);
 * // "&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;"
 * }
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
	 *           the string to sanitize
	 * @return the escaped string, or {@code null} if input is {@code null}
	 */
	@Override
	public String sanitize(final String in) {
		if (in == null) {
			return null;
		}
		return in.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'",
				"&#x27;");
	}
}
