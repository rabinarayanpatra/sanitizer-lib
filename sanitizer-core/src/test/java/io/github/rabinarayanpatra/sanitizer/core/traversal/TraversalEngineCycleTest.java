package io.github.rabinarayanpatra.sanitizer.core.traversal;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeout;

class TraversalEngineCycleTest {

	@Test
	void bidirectionalCycleTerminatesAndLabelsSanitizedOnce() {
		final Node a = new Node();
		a.label = "  A  ";
		final Node b = new Node();
		b.label = "  B  ";
		a.next = b;
		b.next = a;
		assertTimeout(java.time.Duration.ofSeconds(2), () -> SanitizationUtils.apply(a));
		assertEquals("a", a.label);
		assertEquals("b", b.label);
		assertSame(b, a.next);
		assertSame(a, b.next);
	}

	static class Node {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String label;
		@Sanitize(cascade = true)
		Node next;
	}
}
