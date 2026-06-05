package io.github.rabinarayanpatra.sanitizer.core.traversal;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraversalEngineSafetyCheckerTest {

	@Test
	void checkerReturningFalseSkipsDescentButRunsSelfSanitizers() {
		final Parent p = new Parent();
		p.name = "  PARENT  ";
		p.child = new Child();
		p.child.name = "  CHILD  ";
		final TraversalSafetyChecker neverDescend = (parent, field) -> false;
		SanitizationUtils.applyAndReturn(p, neverDescend);
		assertEquals("parent", p.name);
		// Cascade skipped → child name untouched.
		assertEquals("  CHILD  ", p.child.name);
	}

	@Test
	void checkerReturningTrueAllowsDescent() {
		final Parent p = new Parent();
		p.name = "  PARENT  ";
		p.child = new Child();
		p.child.name = "  CHILD  ";
		SanitizationUtils.applyAndReturn(p, TraversalSafetyChecker.ALWAYS);
		assertEquals("parent", p.name);
		assertEquals("child", p.child.name);
	}

	@Test
	void cascadeFieldWithChainReturningNullSkipsDescent() {
		// Covers the (sanitized != null) middle clause of walkPojo's cascade
		// guard: a sanitizer that returns null on a cascade=true POJO field
		// should write back null and skip descent (no NPE).
		final NullableHolder b = new NullableHolder();
		b.child = new Child();
		b.child.name = "  CHILD  ";
		SanitizationUtils.applyAndReturn(b, TraversalSafetyChecker.ALWAYS);
		// child is replaced with null because the chain returned null.
		org.junit.jupiter.api.Assertions.assertNull(b.child);
	}

	public static class NullingChildSanitizer
			implements
				io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer<Child> {
		public NullingChildSanitizer() {
		}

		@Override
		public @org.jspecify.annotations.Nullable Child sanitize(final @org.jspecify.annotations.Nullable Child input) {
			return null;
		}
	}

	static class NullableHolder {
		@Sanitize(using = NullingChildSanitizer.class, cascade = true)
		Child child;
	}

	static class Parent {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
		@Sanitize(cascade = true)
		Child child;
	}

	static class Child {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
	}
}
