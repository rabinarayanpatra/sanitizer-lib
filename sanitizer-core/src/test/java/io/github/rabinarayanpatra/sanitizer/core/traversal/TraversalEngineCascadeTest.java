package io.github.rabinarayanpatra.sanitizer.core.traversal;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TraversalEngineCascadeTest {

	@Test
	void cascadeFalseLeavesNestedAnnotatedFieldUntouched_backwardCompat() {
		final ParentNoCascade parent = new ParentNoCascade();
		parent.name = "  PARENT  ";
		parent.child = new Child();
		parent.child.name = "  CHILD  ";
		SanitizationUtils.apply(parent);
		assertEquals("parent", parent.name);
		// No cascade → child not touched
		assertEquals("  CHILD  ", parent.child.name);
	}

	@Test
	void cascadeTrueIntoPojoMutatesChildInPlace() {
		final ParentWithCascade parent = new ParentWithCascade();
		parent.name = "  PARENT  ";
		parent.child = new Child();
		parent.child.name = "  CHILD  ";
		SanitizationUtils.apply(parent);
		assertEquals("parent", parent.name);
		assertEquals("child", parent.child.name);
	}

	@Test
	void cascadeTrueIntoRecordReplacesField() {
		final HoldsRecord parent = new HoldsRecord();
		parent.embedded = new Embedded("  HELLO  ");
		SanitizationUtils.apply(parent);
		assertNotSame("  HELLO  ", parent.embedded.value());
		assertEquals("HELLO", parent.embedded.value());
	}

	@Test
	void recordRootCascadesIntoChildRecord() {
		final OuterRec input = new OuterRec(new InnerRec("  hi  "));
		final OuterRec out = SanitizationUtils.applyAndReturn(input);
		assertNotSame(input, out);
		assertEquals("HI", out.inner().value());
	}

	@Test
	void cascadeOnlyAnnotationDescendsWithoutSelfSanitizer() {
		final OnlyCascade parent = new OnlyCascade();
		parent.child = new Child();
		parent.child.name = "  CHILD  ";
		SanitizationUtils.apply(parent);
		assertEquals("child", parent.child.name);
	}

	@Test
	void sharedRecordReferenceProducesSameReconstructedInstance() {
		final Shared input = new Shared(new InnerRec("  hi  "));
		final HoldsTwo parent = new HoldsTwo();
		parent.left = input;
		parent.right = input;
		SanitizationUtils.apply(parent);
		// The shared reference inside both Shared records is the same after
		// reconstruction.
		assertSame(parent.left.inner(), parent.right.inner());
		assertEquals("HI", parent.left.inner().value());
	}

	static class ParentNoCascade {
		@Sanitize(using = {TrimSanitizer.class, io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer.class})
		String name;
		Child child;
	}

	static class ParentWithCascade {
		@Sanitize(using = {TrimSanitizer.class, io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer.class})
		String name;
		@Sanitize(cascade = true)
		Child child;
	}

	static class Child {
		@Sanitize(using = {TrimSanitizer.class, io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer.class})
		String name;
	}

	static class HoldsRecord {
		@Sanitize(cascade = true)
		Embedded embedded;
	}

	record Embedded(@Sanitize(using = {
			TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {
	}

	record OuterRec(@Sanitize(cascade = true) InnerRec inner) {
	}

	record InnerRec(@Sanitize(using = {
			TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {
	}

	static class OnlyCascade {
		@Sanitize(cascade = true)
		Child child;
	}

	record Shared(@Sanitize(cascade = true) InnerRec inner) {
	}

	static class HoldsTwo {
		@Sanitize(cascade = true)
		Shared left;
		@Sanitize(cascade = true)
		Shared right;
	}
}
