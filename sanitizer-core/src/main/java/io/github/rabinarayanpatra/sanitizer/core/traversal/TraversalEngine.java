package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizerInstantiationException;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * Recursive walker that applies sanitizers and (optionally) descends into a
 * bean's object graph. Public entry point:
 * {@link #walk(Object, TraversalSafetyChecker)}.
 */
public final class TraversalEngine {

	private static final Logger LOG = LoggerFactory.getLogger(TraversalEngine.class);

	private TraversalEngine() {
	}

	public static @Nullable Object walk(final @Nullable Object node, final TraversalSafetyChecker checker) {
		return walk(node, new TraversalState(), checker);
	}

	static @Nullable Object walk(final @Nullable Object node, final TraversalState state,
			final TraversalSafetyChecker checker) {
		if (node == null) {
			return null;
		}
		final ClassMetadata meta = ClassMetadata.of(node.getClass());
		if (meta.isRecord()) {
			final Object cached = state.findReconstructed(node);
			if (cached != null) {
				return cached;
			}
			final Object built = walkRecord(node, meta, state, checker);
			state.storeReconstructed(node, built);
			return built;
		}
		if (!state.markVisited(node)) {
			return node;
		}
		walkPojo(node, meta, state, checker);
		return node;
	}

	private static Object walkRecord(final Object node, final ClassMetadata meta, final TraversalState state,
			final TraversalSafetyChecker checker) {
		LOG.debug("walk record class={} components={}", node.getClass().getName(), meta.fields().size());
		final RecordComponent[] components = meta.components();
		final Object[] args = new Object[components.length];
		for (int i = 0; i < components.length; i++) {
			final RecordComponent rc = components[i];
			final FieldDescriptor d = meta.fields().get(i);
			try {
				final Object raw = rc.getAccessor().invoke(node);
				Object current = applyChain(raw, d.chain());
				if (d.cascade() && current != null) {
					current = descendByKind(current, d, state, checker);
				}
				args[i] = current;
			} catch (final IllegalAccessException e) {
				throw new IllegalStateException(
						"Cannot invoke accessor for component '" + rc.getName() + "' on " + node.getClass().getName(),
						e);
			} catch (final InvocationTargetException e) {
				rethrowRuntime(e, "Record accessor threw");
			} catch (final ClassCastException e) {
				throw new IllegalStateException("Type mismatch: sanitizer chain incompatible with record component '"
						+ rc.getName() + "' of type " + rc.getType().getName() + " on " + node.getClass().getName(), e);
			}
		}
		try {
			return meta.canonicalCtor().newInstance(args);
		} catch (final InvocationTargetException e) {
			rethrowRuntime(e, "Record canonical constructor threw checked exception");
			return null; // unreachable
		} catch (final ReflectiveOperationException e) {
			throw new SanitizerInstantiationException("Cannot reconstruct record " + node.getClass().getName(), e);
		}
	}

	private static void walkPojo(final Object node, final ClassMetadata meta, final TraversalState state,
			final TraversalSafetyChecker checker) {
		LOG.debug("walk pojo class={} fields={}", node.getClass().getName(), meta.fields().size());
		for (final FieldDescriptor d : meta.fields()) {
			final Field field = d.field();
			if (field == null) {
				continue;
			}
			try {
				final Object raw = field.get(node);
				final Object sanitized = applyChain(raw, d.chain());
				writeBackIfChanged(node, field, raw, sanitized);
				if (d.cascade() && sanitized != null && checker.shouldDescend(node, field)) {
					final Object after = descendByKind(sanitized, d, state, checker);
					if (after != sanitized) {
						field.set(node, after);
					}
				}
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

	private static Object descendByKind(final Object child, final FieldDescriptor d, final TraversalState state,
			final TraversalSafetyChecker checker) {
		switch (d.kind()) {
			case POJO -> {
				walk(child, state, checker);
				return child;
			}
			case RECORD -> {
				final Object replaced = walk(child, state, checker);
				return replaced == null ? child : replaced;
			}
			case COLLECTION, MAP -> {
				// Implemented in Tasks 11 and 12.
				LOG.warn("cascade into {} not yet implemented; skipping", d.kind());
				return child;
			}
			case LEAF -> throw new IllegalStateException("Internal error: cascade descent reached LEAF for "
					+ (d.field() != null ? d.field().getName() : d.recordComponent().getName()));
		}
		return child;
	}

	private static @Nullable Object applyChain(final @Nullable Object raw, final List<FieldSanitizer<Object>> chain) {
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

	private static void rethrowRuntime(final InvocationTargetException e, final String fallback) {
		final Throwable cause = e.getCause() != null ? e.getCause() : e;
		if (cause instanceof RuntimeException re) {
			throw re;
		}
		throw new IllegalStateException(fallback, cause);
	}
}
