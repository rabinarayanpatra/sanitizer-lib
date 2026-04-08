package io.github.rabinarayanpatra.sanitizer.core;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurableFieldSanitizerTest {

	@Test
	void getParam_returnsDefaultWhenMissing() {
		final TestSanitizer s = new TestSanitizer();
		s.configure(Map.of("existing", "value"));
		assertEquals("fallback", s.exposeGetParam("missing", "fallback"));
		assertEquals("value", s.exposeGetParam("existing", "fallback"));
	}

	@Test
	void getIntParam_returnsDefaultWhenMissing() {
		final TestSanitizer s = new TestSanitizer();
		s.configure(Map.of());
		assertEquals(42, s.exposeGetInt("missing", 42));
	}

	@Test
	void getIntParam_returnsDefaultWhenNotParseable() {
		final TestSanitizer s = new TestSanitizer();
		s.configure(Map.of("count", "not-a-number"));
		assertEquals(7, s.exposeGetInt("count", 7));
	}

	@Test
	void getIntParam_parsesValidInt() {
		final TestSanitizer s = new TestSanitizer();
		s.configure(Map.of("count", "15"));
		assertEquals(15, s.exposeGetInt("count", 0));
	}

	@Test
	void parseParams_returnsEmptyForNull() {
		assertTrue(ConfigurableFieldSanitizer.parseParams(null).isEmpty());
	}

	@Test
	void parseParams_returnsEmptyForBlank() {
		assertTrue(ConfigurableFieldSanitizer.parseParams("   ").isEmpty());
	}

	@Test
	void parseParams_skipsEntriesWithoutEqualsSign() {
		final Map<String, String> parsed = ConfigurableFieldSanitizer.parseParams("noequals,key=value");
		assertEquals(1, parsed.size());
		assertEquals("value", parsed.get("key"));
	}

	@Test
	void parseParams_skipsEntriesStartingWithEquals() {
		// eq == 0 → should be skipped (eq > 0 branch false side).
		final Map<String, String> parsed = ConfigurableFieldSanitizer.parseParams("=orphan,key=value");
		assertEquals(1, parsed.size());
		assertEquals("value", parsed.get("key"));
	}

	@Test
	void parseParams_trimsKeysAndValues() {
		final Map<String, String> parsed = ConfigurableFieldSanitizer.parseParams("  key  =  value  ");
		assertEquals("value", parsed.get("key"));
	}

	@Test
	void getParams_returnsUnmodifiableSnapshot() {
		final TestSanitizer s = new TestSanitizer();
		s.configure(Map.of("k", "v"));
		assertEquals("v", s.exposeGetParams().get("k"));
	}

	/** Test fixture exposing protected accessors. */
	private static final class TestSanitizer extends ConfigurableFieldSanitizer<String> {
		@Override
		public @Nullable String sanitize(final @Nullable String input) {
			return input;
		}

		String exposeGetParam(final String key, final String def) {
			return getParam(key, def);
		}

		int exposeGetInt(final String key, final int def) {
			return getIntParam(key, def);
		}

		Map<String, String> exposeGetParams() {
			return getParams();
		}
	}
}
