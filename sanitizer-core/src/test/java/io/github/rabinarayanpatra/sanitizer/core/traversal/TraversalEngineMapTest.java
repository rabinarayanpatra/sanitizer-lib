package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TraversalEngineMapTest {

	@Test
	void mapOfStringToRecordRebuiltWithSanitizedValues() {
		final Bean b = new Bean();
		b.byKey = new LinkedHashMap<>();
		b.byKey.put("a", new Item("  HI  "));
		b.byKey.put("b", new Item("  YO  "));
		final Map<String, Item> before = b.byKey;
		SanitizationUtils.apply(b);
		// Map ref may stay the same (mutable HashMap), values are replaced.
		assertEquals("HI", b.byKey.get("a").value());
		assertEquals("YO", b.byKey.get("b").value());
		assertSame(before, b.byKey);
	}

	@Test
	void mapKeysUntouched() {
		final Bean b = new Bean();
		b.byKey = new HashMap<>();
		b.byKey.put("  KEY  ", new Item("  V  "));
		SanitizationUtils.apply(b);
		// Keys remain untouched; the original raw key is still present.
		assertEquals(1, b.byKey.size());
		assertSame("  KEY  ", b.byKey.keySet().iterator().next());
		assertEquals("V", b.byKey.get("  KEY  ").value());
	}

	@Test
	void unmodifiableMapRebuilt() {
		final Bean b = new Bean();
		b.byKey = Map.of("a", new Item("  X  "));
		final Map<String, Item> before = b.byKey;
		SanitizationUtils.apply(b);
		assertNotSame(before, b.byKey);
		assertEquals("X", b.byKey.get("a").value());
	}

	@Test
	void collectionsUnmodifiableMapRebuilt() {
		// Collections.unmodifiableMap wraps in Collections$UnmodifiableMap;
		// MapWalker#isUnmodifiable handles both ImmutableCollections$ (Map.of)
		// and Collections$Unmodifiable* via separate string prefixes.
		final Bean b = new Bean();
		final Map<String, Item> wrapped = Collections
				.unmodifiableMap(new LinkedHashMap<>(Map.of("a", new Item("  Y  "))));
		b.byKey = wrapped;
		SanitizationUtils.apply(b);
		assertNotSame(wrapped, b.byKey);
		assertEquals("Y", b.byKey.get("a").value());
	}

	@Test
	void rawMapFieldWithCascadeIsSkippedGracefully() {
		// Covers MapWalker#walk's valueType==null short-circuit (raw Map: no
		// resolvable value type).
		final RawHolder b = new RawHolder();
		b.raw = new LinkedHashMap<>();
		b.raw.put("k", new Item("  Z  "));
		SanitizationUtils.apply(b);
		// Item not sanitized because cascade was skipped.
		assertEquals("  Z  ", ((Item) b.raw.get("k")).value());
	}

	@Test
	void mapWithNullValueIsPreservedAsNull() {
		// Triggers MapWalker#processValue null-raw branch.
		final Bean b = new Bean();
		b.byKey = new LinkedHashMap<>();
		b.byKey.put("present", new Item("  X  "));
		b.byKey.put("missing", null);
		SanitizationUtils.apply(b);
		assertEquals("X", b.byKey.get("present").value());
		assertNull(b.byKey.get("missing"));
	}

	@Test
	void mapOfStringValuesAppliesChainOnly() {
		// Covers MapWalker#processValue's chain loop AND the branch where
		// valueType is a String (skip recursive walk).
		final StringValueBean b = new StringValueBean();
		b.byKey = new LinkedHashMap<>();
		b.byKey.put("k1", "  ONE  ");
		b.byKey.put("k2", "  TWO  ");
		SanitizationUtils.apply(b);
		assertEquals("one", b.byKey.get("k1"));
		assertEquals("two", b.byKey.get("k2"));
	}

	static class Bean {
		@Sanitize(cascade = true)
		Map<String, Item> byKey;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static class RawHolder {
		@Sanitize(cascade = true)
		Map raw;
	}

	static class StringValueBean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		Map<String, String> byKey;
	}

	record Item(@Sanitize(using = {
			TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {
	}
}
