package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * Recursive walker that applies sanitizers and (optionally) descends into a
 * bean's object graph. Entry point:
 * {@link #walk(Object, TraversalState, TraversalSafetyChecker)}.
 */
public final class TraversalEngine {

	private static final Logger LOG = LoggerFactory.getLogger(TraversalEngine.class);

	private TraversalEngine() {
	}

	public static @Nullable Object walk(final @Nullable Object node, final TraversalState state,
			final TraversalSafetyChecker checker) {
		if (node == null) {
			return null;
		}
		final ClassMetadata meta = ClassMetadata.of(node.getClass());
		if (meta.isRecord()) {
			throw new UnsupportedOperationException(
					"Records reached TraversalEngine but record support is not yet wired up: "
							+ node.getClass().getName());
		}
		if (!state.markVisited(node)) {
			return node;
		}
		walkPojo(node, meta);
		return node;
	}

	private static void walkPojo(final Object node, final ClassMetadata meta) {
		LOG.debug("walk class={} descriptors={}", node.getClass().getName(), meta.fields().size());
		for (final FieldDescriptor d : meta.fields()) {
			final Field field = d.field();
			if (field == null) {
				continue;
			}
			try {
				final Object raw = field.get(node);
				final Object sanitized = applyChain(raw, d.chain());
				writeBackIfChanged(node, field, raw, sanitized);
			} catch (final IllegalAccessException e) {
				throw new IllegalStateException("Cannot access field '" + field.getName() + "' on "
						+ node.getClass().getName() + ". Ensure the field is mutable and accessible.", e);
			} catch (final ClassCastException e) {
				throw new IllegalStateException("Type mismatch: sanitizer chain incompatible with field '"
						+ field.getName() + "' of type " + field.getType().getName() + " on "
						+ node.getClass().getName() + ". Ensure the sanitizer's generic type matches the field type.",
						e);
			}
		}
	}

	private static @Nullable Object applyChain(final @Nullable Object raw,
			final java.util.List<FieldSanitizer<Object>> chain) {
		Object current = raw;
		for (final FieldSanitizer<Object> s : chain) {
			current = s.sanitize(current);
		}
		return current;
	}

	private static void writeBackIfChanged(final Object node, final Field field, final @Nullable Object raw,
			final @Nullable Object sanitized) throws IllegalAccessException {
		if (raw == null && sanitized == null) {
			return;
		}
		field.set(node, sanitized);
	}
}
