package io.github.rabinarayanpatra.sanitizer.builtin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SafeFilenameSanitizerTest {

	private final SafeFilenameSanitizer sanitizer = new SafeFilenameSanitizer();

	@Test
	void sanitize_returnsNullForNull() {
		assertNull(sanitizer.sanitize(null));
	}

	@Test
	void sanitize_replacesReservedChars() {
		assertEquals("user_data_report_2025_.pdf", sanitizer.sanitize("user:data/report|2025?.pdf"));
	}

	@Test
	void sanitize_replacesBackslash() {
		assertEquals("path_to_file.txt", sanitizer.sanitize("path\\to\\file.txt"));
	}

	@Test
	void sanitize_preservesSafeFilename() {
		assertEquals("report-2025.pdf", sanitizer.sanitize("report-2025.pdf"));
	}

	@Test
	void sanitize_trimsWhitespace() {
		assertEquals("file.txt", sanitizer.sanitize("  file.txt  "));
	}
}
