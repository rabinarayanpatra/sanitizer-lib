package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

	static class Bean {
		@Sanitize(cascade = true)
		Map<String, Item> byKey;
	}

	record Item(@Sanitize(using = {
			TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {
	}
}
