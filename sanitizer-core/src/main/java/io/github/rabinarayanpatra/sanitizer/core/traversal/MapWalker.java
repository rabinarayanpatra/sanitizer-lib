package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/** Cascade descent into {@link Map} fields. Keys are never inspected. */
final class MapWalker {

	private static final Logger LOG = LoggerFactory.getLogger(MapWalker.class);

	private MapWalker() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static Map<?, ?> walk(final Map<?, ?> map, final @Nullable Class<?> valueType,
			final List<FieldSanitizer<Object>> chain, final TraversalState state,
			final TraversalSafetyChecker checker) {
		if (valueType == null) {
			LOG.warn("map field has no resolvable value type; skipping cascade");
			return map;
		}
		final boolean rebuild = isUnmodifiable(map);
		if (rebuild) {
			final Map<Object, Object> target = new LinkedHashMap<>(map.size());
			for (final Map.Entry<?, ?> e : map.entrySet()) {
				target.put(e.getKey(), processValue(e.getValue(), valueType, chain, state, checker));
			}
			return target;
		}
		// Mutable: walk values in place via replaceAll so the map ref is preserved.
		((Map) map).replaceAll((k, v) -> processValue(v, valueType, chain, state, checker));
		return map;
	}

	private static @Nullable Object processValue(final @Nullable Object raw, final Class<?> valueType,
			final List<FieldSanitizer<Object>> chain, final TraversalState state,
			final TraversalSafetyChecker checker) {
		if (raw == null) {
			return null;
		}
		Object current = raw;
		for (final FieldSanitizer<Object> s : chain) {
			current = s.sanitize(current);
		}
		if (valueType.isRecord() || !(valueType.isPrimitive() || valueType == String.class)) {
			final Object after = TraversalEngine.walk(current, state, checker);
			current = after == null ? current : after;
		}
		return current;
	}

	private static boolean isUnmodifiable(final Map<?, ?> map) {
		final String name = map.getClass().getName();
		return name.startsWith("java.util.ImmutableCollections$")
				|| name.startsWith("java.util.Collections$Unmodifiable");
	}
}
