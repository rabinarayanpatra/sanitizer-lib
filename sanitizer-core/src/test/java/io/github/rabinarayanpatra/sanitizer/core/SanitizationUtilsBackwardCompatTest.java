package io.github.rabinarayanpatra.sanitizer.core;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizationUtilsBackwardCompatTest {

	@Test
	void nestedAnnotatedFieldUntouchedWhenCascadeOmitted() {
		final Parent p = new Parent();
		p.name = "  PARENT  ";
		p.child = new Child();
		p.child.name = "  CHILD  ";
		SanitizationUtils.apply(p);
		assertEquals("parent", p.name);
		// Without cascade=true the engine must not descend.
		assertEquals("  CHILD  ", p.child.name);
	}

	static class Parent {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
		Child child; // no @Sanitize on field → not annotated, not walked
	}

	static class Child {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
	}
}
