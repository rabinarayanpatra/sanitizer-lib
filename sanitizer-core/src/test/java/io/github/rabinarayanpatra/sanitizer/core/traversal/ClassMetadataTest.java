package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassMetadataTest {

	@Test
	void leafTypesResolveToLeafKind() {
		final ClassMetadata meta = ClassMetadata.of(LeafBean.class);
		final Map<String, FieldDescriptor> byName = byFieldName(meta);
		assertEquals(Kind.LEAF, byName.get("text").kind());
		assertEquals(Kind.LEAF, byName.get("count").kind());
		assertEquals(Kind.LEAF, byName.get("boxed").kind());
		assertEquals(Kind.LEAF, byName.get("uuid").kind());
		assertEquals(Kind.LEAF, byName.get("instant").kind());
		assertEquals(Kind.LEAF, byName.get("amount").kind());
		assertEquals(Kind.LEAF, byName.get("color").kind());
	}

	@Test
	void recordIsDetectedAndCanonicalCtorCaptured() {
		final ClassMetadata meta = ClassMetadata.of(SimpleRecord.class);
		assertTrue(meta.isRecord());
		assertNotNull(meta.canonicalCtor());
		assertNotNull(meta.components());
		assertEquals(1, meta.components().length);
		assertEquals(1, meta.fields().size());
		assertEquals(Kind.LEAF, meta.fields().get(0).kind());
	}

	@Test
	void collectionFieldResolvesElementType() {
		final ClassMetadata meta = ClassMetadata.of(CollectionBean.class);
		final Map<String, FieldDescriptor> byName = byFieldName(meta);
		assertEquals(Kind.COLLECTION, byName.get("names").kind());
		assertSame(String.class, byName.get("names").elementType());
		assertEquals(Kind.COLLECTION, byName.get("records").kind());
		assertSame(SimpleRecord.class, byName.get("records").elementType());
		assertEquals(Kind.MAP, byName.get("byName").kind());
		assertSame(SimpleRecord.class, byName.get("byName").elementType());
	}

	@Test
	void pojoFieldIsClassifiedAsPojo() {
		final ClassMetadata meta = ClassMetadata.of(NestedBean.class);
		final Map<String, FieldDescriptor> byName = byFieldName(meta);
		assertEquals(Kind.POJO, byName.get("child").kind());
	}

	@Test
	void cascadeTrueOnLeafFieldThrows() {
		final IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> ClassMetadata.of(IllegalLeafCascade.class));
		assertTrue(ex.getMessage().contains("cascade"));
		assertTrue(ex.getMessage().contains("text"));
		assertTrue(ex.getMessage().contains("LEAF"));
	}

	@Test
	void superclassFieldsWalked() {
		final ClassMetadata meta = ClassMetadata.of(ChildAnnotated.class);
		final Map<String, FieldDescriptor> byName = byFieldName(meta);
		assertNotNull(byName.get("parentName"));
		assertNotNull(byName.get("childName"));
	}

	@Test
	void fieldWithoutSanitizeAnnotationIsSkipped() {
		final ClassMetadata meta = ClassMetadata.of(PartiallyAnnotated.class);
		assertEquals(1, meta.fields().size());
		assertEquals("annotated", meta.fields().get(0).field().getName());
	}

	@Test
	void emptyUsingArrayWithCascadeFalseIsRejected() {
		final IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> ClassMetadata.of(NoOpAnnotation.class));
		assertTrue(ex.getMessage().contains("no sanitizer and cascade=false"));
	}

	@Test
	void rawCollectionTypeReturnsNullElementType() {
		final ClassMetadata meta = ClassMetadata.of(RawCollectionBean.class);
		final FieldDescriptor d = meta.fields().get(0);
		assertEquals(Kind.COLLECTION, d.kind());
		assertNull(d.elementType());
	}

	@Test
	void typeReturnsTheBackingClass() {
		// Covers the trivial accessor that was previously unreferenced.
		final ClassMetadata meta = ClassMetadata.of(SimpleRecord.class);
		assertSame(SimpleRecord.class, meta.type());
	}

	@Test
	void nestedParameterizedElementResolvesToOuterRaw() {
		// List<Map<String,String>>: target arg is a ParameterizedType, so
		// resolveElementType returns the raw type (Map.class).
		final ClassMetadata meta = ClassMetadata.of(NestedGenericBean.class);
		final FieldDescriptor d = meta.fields().get(0);
		assertEquals(Kind.COLLECTION, d.kind());
		assertSame(Map.class, d.elementType());
	}

	@Test
	void wildcardElementResolvesToNullElementType() {
		// List<? extends SimpleRecord>: the type arg is a WildcardType (neither
		// Class nor ParameterizedType) so elementType is null and the engine
		// skips cascading at runtime.
		final ClassMetadata meta = ClassMetadata.of(WildcardElementBean.class);
		final FieldDescriptor d = meta.fields().get(0);
		assertEquals(Kind.COLLECTION, d.kind());
		assertNull(d.elementType());
	}

	private static Map<String, FieldDescriptor> byFieldName(final ClassMetadata meta) {
		final java.util.Map<String, FieldDescriptor> m = new java.util.LinkedHashMap<>();
		for (final FieldDescriptor d : meta.fields()) {
			m.put(d.field() != null ? d.field().getName() : d.recordComponent().getName(), d);
		}
		return m;
	}

	enum Color {
		RED, BLUE
	}

	static class LeafBean {
		@Sanitize(using = TrimSanitizer.class)
		String text;
		@Sanitize(using = TrimSanitizer.class)
		int count;
		@Sanitize(using = TrimSanitizer.class)
		Integer boxed;
		@Sanitize(using = TrimSanitizer.class)
		UUID uuid;
		@Sanitize(using = TrimSanitizer.class)
		Instant instant;
		@Sanitize(using = TrimSanitizer.class)
		BigDecimal amount;
		@Sanitize(using = TrimSanitizer.class)
		Color color;
	}

	record SimpleRecord(@Sanitize(using = TrimSanitizer.class) String value) {
	}

	static class CollectionBean {
		@Sanitize(using = LowerCaseSanitizer.class)
		List<String> names;
		@Sanitize(cascade = true)
		Set<SimpleRecord> records;
		@Sanitize(cascade = true)
		Map<String, SimpleRecord> byName;
	}

	static class NestedBean {
		@Sanitize(cascade = true)
		LeafBean child;
	}

	static class IllegalLeafCascade {
		@Sanitize(cascade = true)
		String text;
	}

	static class ParentAnnotated {
		@Sanitize(using = TrimSanitizer.class)
		String parentName;
	}

	static class ChildAnnotated extends ParentAnnotated {
		@Sanitize(using = UpperCaseSanitizer.class)
		String childName;
	}

	static class PartiallyAnnotated {
		String unannotated;
		@Sanitize(using = TrimSanitizer.class)
		String annotated;
	}

	static class NoOpAnnotation {
		@Sanitize
		String pointless;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	static class RawCollectionBean {
		@Sanitize(cascade = true)
		List raw;
	}

	static class NestedGenericBean {
		@Sanitize(cascade = true)
		List<Map<String, String>> nested;
	}

	static class WildcardElementBean {
		@Sanitize(cascade = true)
		List<? extends SimpleRecord> items;
	}
}
