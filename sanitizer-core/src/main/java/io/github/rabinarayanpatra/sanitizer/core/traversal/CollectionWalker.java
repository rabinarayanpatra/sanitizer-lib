package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/** Cascade descent into {@link Collection} fields. */
final class CollectionWalker {

	private static final Logger LOG = LoggerFactory.getLogger(CollectionWalker.class);

	private CollectionWalker() {
	}

	/**
	 * Walks a collection. Returns the same reference when elements are mutated in
	 * place; returns a new collection when the original is unmodifiable or when
	 * elements are records (immutable).
	 */
	@SuppressWarnings("unchecked")
	static Collection<?> walk(final Collection<?> coll, final @Nullable Class<?> elementType,
			final List<FieldSanitizer<Object>> chain, final TraversalState state,
			final TraversalSafetyChecker checker) {
		if (elementType == null) {
			LOG.warn("collection field has no resolvable element type; skipping cascade");
			return coll;
		}
		final Kind elementKind = kindOf(elementType);
		final boolean isUnmodifiable = isUnmodifiable(coll);
		final boolean rebuild = isUnmodifiable || elementKind == Kind.RECORD;

		if (rebuild) {
			return rebuild(coll, elementKind, chain, state, checker);
		}
		// Mutable in-place path
		if (coll instanceof List<?>) {
			walkListInPlace((List<Object>) coll, elementKind, chain, state, checker);
			return coll;
		}
		if (coll instanceof Set<?>) {
			// Sets: rebuild even when mutable, because hash codes may change for
			// records and leaf-sanitized strings.
			return rebuild(coll, elementKind, chain, state, checker);
		}
		// Other Collection: rebuild as ArrayList
		return rebuild(coll, elementKind, chain, state, checker);
	}

	private static void walkListInPlace(final List<Object> list, final Kind elementKind,
			final List<FieldSanitizer<Object>> chain, final TraversalState state,
			final TraversalSafetyChecker checker) {
		for (int i = 0; i < list.size(); i++) {
			final Object raw = list.get(i);
			final Object processed = processElement(raw, elementKind, chain, state, checker);
			if (processed != raw) {
				list.set(i, processed);
			}
		}
	}

	private static Collection<?> rebuild(final Collection<?> source, final Kind elementKind,
			final List<FieldSanitizer<Object>> chain, final TraversalState state,
			final TraversalSafetyChecker checker) {
		final Collection<Object> target = source instanceof Set<?>
				? new LinkedHashSet<>(source.size())
				: new ArrayList<>(source.size());
		for (final Object raw : source) {
			target.add(processElement(raw, elementKind, chain, state, checker));
		}
		return target;
	}

	private static @Nullable Object processElement(final @Nullable Object raw, final Kind elementKind,
			final List<FieldSanitizer<Object>> chain, final TraversalState state,
			final TraversalSafetyChecker checker) {
		if (raw == null) {
			return null;
		}
		Object current = raw;
		for (final FieldSanitizer<Object> s : chain) {
			current = s.sanitize(current);
		}
		if (elementKind == Kind.POJO || elementKind == Kind.RECORD) {
			final Object after = TraversalEngine.walk(current, state, checker);
			current = after == null ? current : after;
		}
		return current;
	}

	private static Kind kindOf(final Class<?> elementType) {
		if (elementType.isRecord()) {
			return Kind.RECORD;
		}
		if (Collection.class.isAssignableFrom(elementType) || java.util.Map.class.isAssignableFrom(elementType)) {
			// Nested collections within collections are out of scope for v1.2.0;
			// treat as POJO so the engine descends into the wrapper.
			return Kind.POJO;
		}
		if (elementType.isPrimitive() || elementType.isEnum() || elementType == String.class
				|| elementType == Integer.class || elementType == Long.class || elementType == Boolean.class
				|| elementType == java.util.UUID.class || elementType == java.time.Instant.class) {
			return Kind.LEAF;
		}
		return Kind.POJO;
	}

	private static boolean isUnmodifiable(final Collection<?> coll) {
		final String name = coll.getClass().getName();
		return name.startsWith("java.util.ImmutableCollections$")
				|| name.startsWith("java.util.Collections$Unmodifiable");
	}
}
