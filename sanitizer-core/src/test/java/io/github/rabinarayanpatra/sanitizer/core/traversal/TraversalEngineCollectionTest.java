package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalEngineCollectionTest {

	@Test
	void listOfStringsLeafSanitizedInPlace() {
		final WithStringList b = new WithStringList();
		b.values = new ArrayList<>(List.of("  A  ", "  B  "));
		final List<String> before = b.values;
		SanitizationUtils.apply(b);
		assertSame(before, b.values);
		assertEquals(List.of("a", "b"), b.values);
	}

	@Test
	void unmodifiableListOfStringsLeafSanitizedRebuiltAsArrayList() {
		final WithStringList b = new WithStringList();
		b.values = List.of("  A  ", "  B  ");
		final List<String> before = b.values;
		SanitizationUtils.apply(b);
		assertNotSame(before, b.values);
		assertTrue(b.values instanceof ArrayList<?>);
		assertEquals(List.of("a", "b"), b.values);
	}

	@Test
	void listOfRecordsRebuiltWithSanitizedInstances() {
		final WithRecordList b = new WithRecordList();
		b.items = new ArrayList<>(List.of(new Item("  HI  "), new Item("  YO  ")));
		final List<Item> before = b.items;
		SanitizationUtils.apply(b);
		assertNotSame(before, b.items);
		assertEquals("HI", b.items.get(0).value());
		assertEquals("YO", b.items.get(1).value());
	}

	@Test
	void listOfMutablePojosWalkedInPlace() {
		final WithPojoList b = new WithPojoList();
		b.children = new ArrayList<>(List.of(new Child("  A  "), new Child("  B  ")));
		final List<Child> before = b.children;
		final Child first = before.get(0);
		SanitizationUtils.apply(b);
		assertSame(before, b.children);
		assertSame(first, b.children.get(0));
		assertEquals("a", b.children.get(0).name);
		assertEquals("b", b.children.get(1).name);
	}

	@Test
	void setOfStringsRebuiltAsLinkedHashSetPreservingOrder() {
		final WithStringSet b = new WithStringSet();
		b.values = new LinkedHashSet<>(List.of("  A  ", "  B  ", "  C  "));
		SanitizationUtils.apply(b);
		final Iterator<String> it = b.values.iterator();
		assertEquals("a", it.next());
		assertEquals("b", it.next());
		assertEquals("c", it.next());
	}

	@Test
	void unmodifiableSetOfRecordsRebuilt() {
		final WithRecordSet b = new WithRecordSet();
		b.items = Set.of(new Item("  HI  "));
		SanitizationUtils.apply(b);
		assertEquals(1, b.items.size());
		assertEquals("HI", b.items.iterator().next().value());
	}

	@Test
	void rawCollectionFieldWithCascadeIsSkippedGracefully() {
		// No element type → cascade is a no-op; should not throw.
		final RawHolder b = new RawHolder();
		b.raw = new ArrayList<Object>(List.of("ignored"));
		SanitizationUtils.apply(b);
		assertEquals(List.of("ignored"), b.raw);
	}

	static class WithStringList {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		List<String> values;
	}

	static class WithRecordList {
		@Sanitize(cascade = true)
		List<Item> items;
	}

	static class WithPojoList {
		@Sanitize(cascade = true)
		List<Child> children;
	}

	static class WithStringSet {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		Set<String> values;
	}

	static class WithRecordSet {
		@Sanitize(cascade = true)
		Set<Item> items;
	}

	record Item(@Sanitize(using = {
			TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {
	}

	static class Child {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		String name;

		Child() {
		}

		Child(final String name) {
			this.name = name;
		}
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static class RawHolder {
		@Sanitize(cascade = true)
		List raw;
	}
}
