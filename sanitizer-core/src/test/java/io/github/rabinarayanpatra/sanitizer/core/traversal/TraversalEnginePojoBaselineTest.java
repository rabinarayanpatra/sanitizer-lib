package io.github.rabinarayanpatra.sanitizer.core.traversal;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class TraversalEnginePojoBaselineTest {

	@Test
	void walkSanitizesPojoFieldAndReturnsSameRef() {
		final Bean b = new Bean();
		b.name = "  HELLO  ";
		final Object result = TraversalEngine.walk(b, new TraversalState(), TraversalSafetyChecker.ALWAYS);
		assertSame(b, result);
		assertEquals("hello", b.name);
	}

	@Test
	void walkNullReturnsNull() {
		assertNull(TraversalEngine.walk(null, new TraversalState(), TraversalSafetyChecker.ALWAYS));
	}

	@Test
	void walkPojoWithNullFieldKeepsNull() {
		final Bean b = new Bean();
		b.name = null;
		TraversalEngine.walk(b, new TraversalState(), TraversalSafetyChecker.ALWAYS);
		assertNull(b.name);
	}

	static class Bean {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
	}
}
