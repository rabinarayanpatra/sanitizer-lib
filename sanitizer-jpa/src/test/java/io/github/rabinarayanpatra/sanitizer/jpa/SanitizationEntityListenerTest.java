package io.github.rabinarayanpatra.sanitizer.jpa;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizationEntityListenerTest {

	@Test
	void onSaveSanitizesTopLevelFields() {
		final Entity e = new Entity();
		e.name = "  HELLO  ";
		new SanitizationEntityListener().onSave(e);
		assertEquals("hello", e.name);
	}

	@Test
	void customSafetyCheckerIsHonored() {
		final Parent p = new Parent();
		p.name = "  PARENT  ";
		p.child = new Child();
		p.child.name = "  CHILD  ";
		final SanitizationEntityListener listener = new SanitizationEntityListener();
		listener.setSafetyChecker((parent, field) -> false);
		listener.onSave(p);
		assertEquals("parent", p.name);
		assertEquals("  CHILD  ", p.child.name);
	}

	@Test
	void defaultSafetyCheckerAllowsDescent() {
		final Parent p = new Parent();
		p.name = "  PARENT  ";
		p.child = new Child();
		p.child.name = "  CHILD  ";
		final SanitizationEntityListener listener = new SanitizationEntityListener();
		listener.setSafetyChecker(TraversalSafetyChecker.ALWAYS);
		listener.onSave(p);
		assertEquals("parent", p.name);
		assertEquals("child", p.child.name);
	}

	static class Entity {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;
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
