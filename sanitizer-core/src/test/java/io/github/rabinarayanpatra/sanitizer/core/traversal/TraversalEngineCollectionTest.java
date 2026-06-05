package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
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

	@Test
	void otherCollectionFieldRebuiltAsArrayList() {
		// ArrayDeque is neither List nor Set → exercises the "other Collection"
		// fallback branch in CollectionWalker.walk. The field is declared as
		// Collection<String> so the rebuilt ArrayList is assignable.
		final WithCollection b = new WithCollection();
		b.values = new ArrayDeque<>(List.of("  A  ", "  B  "));
		final Collection<String> before = b.values;
		SanitizationUtils.apply(b);
		assertNotSame(before, b.values);
		assertEquals(Arrays.asList("a", "b"), new ArrayList<>(b.values));
	}

	@Test
	void listWithNullElementIsPreservedAsNull() {
		// Tests CollectionWalker.processElement's null-raw branch.
		final WithStringList b = new WithStringList();
		final ArrayList<String> values = new ArrayList<>();
		values.add("  A  ");
		values.add(null);
		b.values = values;
		SanitizationUtils.apply(b);
		assertEquals("a", b.values.get(0));
		assertNull(b.values.get(1));
	}

	@Test
	void listOfListsTreatsInnerCollectionAsPojo() {
		// element type is Collection → CollectionWalker.kindOf returns POJO;
		// inner list is walked as a POJO (no @Sanitize fields, no-op) without
		// crashing.
		final WithListOfLists b = new WithListOfLists();
		b.nested = new ArrayList<>();
		b.nested.add(new ArrayList<>(List.of("untouched")));
		SanitizationUtils.apply(b);
		assertEquals(List.of(List.of("untouched")), b.nested);
	}

	@Test
	void pojoListFieldSkippedWhenSafetyCheckerForbidsDescent() {
		// Covers the walkPojo branch where shouldDescend returns false for a
		// collection/map field (cascade=true but no descent).
		final WithRecordList b = new WithRecordList();
		final Item original = new Item("  HI  ");
		b.items = new ArrayList<>(List.of(original));
		final List<Item> before = b.items;
		SanitizationUtils.applyAndReturn(b, (parent, field) -> false);
		assertSame(before, b.items);
		// Element untouched since descent was disallowed.
		assertSame(original, b.items.get(0));
	}

	@Test
	void nullCollectionFieldIsLeftAsNull() {
		// Covers walkPojo's `raw == null` branch on COLLECTION/MAP kind.
		final WithStringList b = new WithStringList();
		b.values = null;
		SanitizationUtils.applyAndReturn(b, TraversalSafetyChecker.ALWAYS);
		assertNull(b.values);
	}

	static class WithCollection {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		Collection<String> values;
	}

	static class WithListOfLists {
		@Sanitize(cascade = true)
		List<Collection<String>> nested;
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
