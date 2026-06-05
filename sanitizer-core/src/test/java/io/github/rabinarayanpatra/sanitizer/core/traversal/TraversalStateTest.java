package io.github.rabinarayanpatra.sanitizer.core.traversal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalStateTest {

	@Test
	void markVisitedReturnsTrueOnFirstVisitAndFalseOnRevisit() {
		final TraversalState state = new TraversalState();
		final Object node = new Object();
		assertTrue(state.markVisited(node));
		assertFalse(state.markVisited(node));
	}

	@Test
	void recordsAreStoredAndRetrievedByIdentity() {
		final TraversalState state = new TraversalState();
		final String original = "a";
		final String rebuilt = "A";
		assertNull(state.findReconstructed(original));
		state.storeReconstructed(original, rebuilt);
		assertSame(rebuilt, state.findReconstructed(original));
	}
}
