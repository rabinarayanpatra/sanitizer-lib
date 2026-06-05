package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.IdentityHashMap;

import org.jspecify.annotations.Nullable;

/**
 * Mutable per-invocation traversal state. Tracks visited POJOs/collections/maps
 * for cycle detection and reconstructed record instances keyed by their
 * original references.
 */
final class TraversalState {

	private final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
	private final IdentityHashMap<Object, Object> reconstructedRecords = new IdentityHashMap<>();

	TraversalState() {
	}

	/**
	 * @return true the first time this node is visited; false on every subsequent
	 *         visit (indicates a cycle).
	 */
	boolean markVisited(final Object node) {
		return visited.put(node, Boolean.TRUE) == null;
	}

	void storeReconstructed(final Object original, final Object rebuilt) {
		reconstructedRecords.put(original, rebuilt);
	}

	@Nullable
	Object findReconstructed(final Object original) {
		return reconstructedRecords.get(original);
	}
}
