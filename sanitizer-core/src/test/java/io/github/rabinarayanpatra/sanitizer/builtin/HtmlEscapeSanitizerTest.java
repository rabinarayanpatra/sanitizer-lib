package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HtmlEscapeSanitizerTest {

	private final HtmlEscapeSanitizer sanitizer = new HtmlEscapeSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_escapesAngleBrackets() {
		assertEquals("&lt;div&gt;", sanitizer.sanitize("<div>"));
	}

	@Test
	void sanitize_escapesAmpersand() {
		assertEquals("a &amp; b", sanitizer.sanitize("a & b"));
	}

	@Test
	void sanitize_escapesQuotes() {
		assertEquals("&quot;hello&quot;", sanitizer.sanitize("\"hello\""));
	}

	@Test
	void sanitize_escapesSingleQuotes() {
		assertEquals("it&#x27;s", sanitizer.sanitize("it's"));
	}

	@Test
	void sanitize_escapesScriptTag() {
		assertEquals("&lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;",
				sanitizer.sanitize("<script>alert('xss')</script>"));
	}

	@Test
	void sanitize_preservesPlainText() {
		assertEquals("hello world", sanitizer.sanitize("hello world"));
	}
}
