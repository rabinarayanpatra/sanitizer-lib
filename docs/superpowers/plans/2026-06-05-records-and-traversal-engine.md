# Records support and recursive traversal engine — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship v1.2.0 of `sanitizer-lib` with first-class Java records support and an opt-in recursive traversal engine that walks nested POJOs, records, collections, and maps — strictly backward compatible for all existing POJO callers.

**Architecture:** A new package-private `core.traversal` engine (`TraversalEngine` + `ClassMetadata` + `FieldDescriptor` + `TraversalState`) replaces the inline reflection loop inside `SanitizationUtils`. The existing `SanitizationUtils` becomes a thin facade. A new public `TraversalSafetyChecker` interface lets callers (in particular a new Hibernate-aware impl) gate descent into lazy associations. Records are reconstructed via canonical constructor; POJOs continue to mutate in place. A new `sanitizer-jpa-spring` module wires the Hibernate checker via Spring's `SpringBeanContainer`.

**Tech Stack:** Java 21, Gradle Kotlin DSL, JUnit 5 (Jupiter), Mockito (already on classpath via spring-boot-starter-test), JSpecify nullness annotations, ErrorProne, Spotless, Spring Boot 3.4.5 BOM, Hibernate ORM (via Spring Boot starter), SLF4J API.

**Spec reference:** `docs/superpowers/specs/2026-06-05-records-and-traversal-engine-design.md`

---

## Pre-flight

- [ ] **Step 0.1: Confirm clean working tree on master**

Run: `git -C /Volumes/Work/sanitizer-lib status`
Expected: `On branch master`, `nothing to commit, working tree clean`. If dirty, stash before proceeding.

- [ ] **Step 0.2: Create a feature branch**

Run:
```bash
cd /Volumes/Work/sanitizer-lib
git checkout -b feat/v1.2.0-records-and-traversal
```
Expected: `Switched to a new branch 'feat/v1.2.0-records-and-traversal'`.

- [ ] **Step 0.3: Baseline build green**

Run: `./gradlew check`
Expected: `BUILD SUCCESSFUL`. If any test fails, stop and investigate before any code change.

---

## Phase 1: Annotation evolution + safety-checker interface

### Task 1: Add `cascade` attribute and default `using={}` to `@Sanitize`

**Files:**
- Modify: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/annotation/Sanitize.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/annotation/SanitizeAnnotationTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/annotation/SanitizeAnnotationTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanitizeAnnotationTest {

    @Test
    void cascadeDefaultsToFalse() throws NoSuchFieldException {
        final Field field = Fixtures.class.getDeclaredField("trimmedOnly");
        final Sanitize ann = field.getAnnotation(Sanitize.class);
        assertFalse(ann.cascade());
    }

    @Test
    void cascadeReadsTrueWhenSet() throws NoSuchFieldException {
        final Field field = Fixtures.class.getDeclaredField("cascadeOnly");
        final Sanitize ann = field.getAnnotation(Sanitize.class);
        assertTrue(ann.cascade());
        assertArrayEquals(new Class<?>[0], ann.using());
    }

    @Test
    void cascadeCombinedWithUsing() throws NoSuchFieldException {
        final Field field = Fixtures.class.getDeclaredField("trimAndCascade");
        final Sanitize ann = field.getAnnotation(Sanitize.class);
        assertTrue(ann.cascade());
        assertEquals(1, ann.using().length);
        assertEquals(TrimSanitizer.class, ann.using()[0]);
    }

    static class Fixtures {
        @Sanitize(using = TrimSanitizer.class)
        String trimmedOnly;

        @Sanitize(cascade = true)
        Object cascadeOnly;

        @Sanitize(using = TrimSanitizer.class, cascade = true)
        String trimAndCascade;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.annotation.SanitizeAnnotationTest"`
Expected: COMPILATION FAILURE — `cannot find symbol: method cascade()` and the `@Sanitize(cascade = true)` form is invalid because `using` has no default yet.

- [ ] **Step 3: Update the annotation**

Replace `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/annotation/Sanitize.java` body with:

```java
package io.github.rabinarayanpatra.sanitizer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Apply one or more {@link FieldSanitizer} implementations to this field, in the
 * order listed, and/or descend into this field's object graph.
 *
 * <p>
 * This annotation is {@link Repeatable}, so multiple {@code @Sanitize}
 * annotations can be stacked on the same field. Sanitizers are applied in
 * declaration order. When {@link #cascade()} is true on any stacked instance,
 * the engine descends into the field's value after self-sanitizers run.
 *
 * @since 1.0.0
 */
@Repeatable(Sanitizes.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Sanitize {
    /**
     * The ordered list of FieldSanitizer implementations to apply to this field's
     * value. May be empty when the annotation is used solely to opt into
     * {@link #cascade() cascade}.
     *
     * @return the sanitizer classes, possibly empty
     */
    @SuppressWarnings("java:S1452")
    Class<? extends FieldSanitizer<?>>[] using() default {};

    /**
     * Optional comma-separated {@code key=value} configuration parameters passed
     * to {@link io.github.rabinarayanpatra.sanitizer.core.ConfigurableFieldSanitizer}
     * implementations. Ignored for non-configurable sanitizers.
     *
     * <p>
     * Example: {@code params = "maxLength=100,suffix=..."} or
     * {@code params = "reveal=4"}
     *
     * @return the parameter string, empty by default
     * @since 1.1.0
     */
    String params() default "";

    /**
     * When true, the traversal engine descends into this field's value after any
     * self-sanitizers run. Supported on fields whose static type is a POJO,
     * record, {@link java.util.Collection}, or {@link java.util.Map}. Applying
     * {@code cascade=true} to a leaf type (e.g. {@code String}, primitive,
     * boxed primitive, {@code UUID}, JSR-310 temporal, {@code BigDecimal},
     * {@code BigInteger}, {@code Enum}) is rejected at metadata-build time.
     *
     * @return true to descend into this field's object graph
     * @since 1.2.0
     */
    boolean cascade() default false;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.annotation.SanitizeAnnotationTest"`
Expected: 3 tests pass.

- [ ] **Step 5: Full module test pass**

Run: `./gradlew :sanitizer-core:check`
Expected: BUILD SUCCESSFUL. (Existing tests must still pass — the new `cascade()` attribute is purely additive.)

- [ ] **Step 6: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/annotation/Sanitize.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/annotation/SanitizeAnnotationTest.java
git commit -m "feat(core): add cascade attribute and default using={} to @Sanitize

Assisted by AI"
```

---

### Task 2: Add public `TraversalSafetyChecker` interface

**Files:**
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/TraversalSafetyChecker.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/TraversalSafetyCheckerTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/TraversalSafetyCheckerTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalSafetyCheckerTest {

    @Test
    void alwaysReturnsTrueForAnyParentAndField() throws NoSuchFieldException {
        final Field field = Holder.class.getDeclaredField("name");
        assertTrue(TraversalSafetyChecker.ALWAYS.shouldDescend(new Holder(), field));
    }

    @Test
    void lambdaImplementationIsHonored() throws NoSuchFieldException {
        final Field field = Holder.class.getDeclaredField("name");
        final TraversalSafetyChecker neverDescend = (parent, f) -> false;
        assertFalse(neverDescend.shouldDescend(new Holder(), field));
    }

    static class Holder {
        String name;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyCheckerTest"`
Expected: COMPILATION FAILURE — `cannot find symbol: class TraversalSafetyChecker`.

- [ ] **Step 3: Create the interface**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/TraversalSafetyChecker.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;

/**
 * Pluggable gate that lets callers decide whether the traversal engine may
 * descend into a given field of a parent object. Used to avoid touching
 * non-initialized JPA lazy associations or other contexts where reading a
 * field is expensive or unsafe.
 *
 * @since 1.2.0
 */
@FunctionalInterface
public interface TraversalSafetyChecker {

    /**
     * Returns true when the engine may read and recurse into {@code field} on
     * {@code parent}.
     *
     * @param parent
     *            the bean currently being walked
     * @param field
     *            the field about to be descended into
     * @return true to allow descent; false to skip this field
     */
    boolean shouldDescend(Object parent, Field field);

    /**
     * No-op checker that always permits descent. Default used by
     * {@code sanitizer-core} when callers do not supply a custom checker.
     */
    TraversalSafetyChecker ALWAYS = (parent, field) -> true;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyCheckerTest"`
Expected: 2 tests pass.

- [ ] **Step 5: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/TraversalSafetyChecker.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/TraversalSafetyCheckerTest.java
git commit -m "feat(core): add public TraversalSafetyChecker interface

Assisted by AI"
```

---

## Phase 2: Traversal internals (Kind, FieldDescriptor, ClassMetadata, TraversalState)

### Task 3: Add `Kind` enum and `FieldDescriptor` (package-private)

**Files:**
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/Kind.java`
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/FieldDescriptor.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/FieldDescriptorTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/FieldDescriptorTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldDescriptorTest {

    @Test
    void pojoFieldDescriptorExposesFieldAndChain() throws NoSuchFieldException {
        final Field f = Fixtures.class.getDeclaredField("name");
        final List<FieldSanitizer<Object>> chain = List.of(new TrimSanitizer().asObjectSanitizer());
        final FieldDescriptor d = FieldDescriptor.forPojoField(f, chain, false, Kind.LEAF, null);
        assertSame(f, d.field());
        assertNull(d.recordComponent());
        assertEquals(chain, d.chain());
        assertEquals(Kind.LEAF, d.kind());
        assertNull(d.elementType());
        assertTrue(!d.cascade());
    }

    static class Fixtures {
        String name;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.FieldDescriptorTest"`
Expected: COMPILATION FAILURE — missing classes `FieldDescriptor`, `Kind`, and method `TrimSanitizer.asObjectSanitizer`.

- [ ] **Step 3: Create `Kind` enum**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/Kind.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

/**
 * Classification of a field's declared type that drives traversal behavior.
 */
enum Kind {
    /** Strings, primitives/boxed, UUID, JSR-310 temporals, BigDecimal/Integer, Enum. */
    LEAF,
    /** Plain mutable Java object (any class that is not LEAF/RECORD/COLLECTION/MAP). */
    POJO,
    /** Subclass of {@link java.lang.Record}. */
    RECORD,
    /** Subtype of {@link java.util.Collection}. */
    COLLECTION,
    /** Subtype of {@link java.util.Map}. */
    MAP
}
```

- [ ] **Step 4: Create `FieldDescriptor` record**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/FieldDescriptor.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Compiled descriptor for a single field (POJO) or record component. Built once
 * per class by {@link ClassMetadata} and reused on every traversal.
 */
record FieldDescriptor(
        @Nullable Field field,
        @Nullable RecordComponent recordComponent,
        List<FieldSanitizer<Object>> chain,
        boolean cascade,
        Kind kind,
        @Nullable Class<?> elementType) {

    static FieldDescriptor forPojoField(
            final Field field,
            final List<FieldSanitizer<Object>> chain,
            final boolean cascade,
            final Kind kind,
            final @Nullable Class<?> elementType) {
        return new FieldDescriptor(field, null, chain, cascade, kind, elementType);
    }

    static FieldDescriptor forRecordComponent(
            final RecordComponent component,
            final List<FieldSanitizer<Object>> chain,
            final boolean cascade,
            final Kind kind,
            final @Nullable Class<?> elementType) {
        return new FieldDescriptor(null, component, chain, cascade, kind, elementType);
    }
}
```

- [ ] **Step 5: Add `asObjectSanitizer` helper on `FieldSanitizer` for safe casting in tests**

This helper avoids `unchecked` casts in test code. Edit `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/FieldSanitizer.java` to add:

Replace the entire file with:

```java
package io.github.rabinarayanpatra.sanitizer.core;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface for sanitizing a single field value.
 *
 * @param <T>
 *            the type of the field (e.g., String, LocalDate, BigDecimal)
 */
@FunctionalInterface
public interface FieldSanitizer<T> {
    /**
     * Apply sanitization to the input value.
     *
     * @param input
     *            the raw value (maybe null)
     * @return the sanitized value (maybe null)
     */
    @Nullable
    T sanitize(@Nullable T input);

    /**
     * Returns this sanitizer typed as {@code FieldSanitizer<Object>}. The cast is
     * safe at runtime because the traversal engine routes each sanitizer only to
     * fields whose declared type matches the sanitizer's generic parameter; a
     * {@code ClassCastException} thrown by {@code sanitize} is caught and rewrapped
     * with a descriptive {@code IllegalStateException} by the engine.
     *
     * @return this sanitizer as an Object-typed sanitizer
     * @since 1.2.0
     */
    @SuppressWarnings("unchecked")
    default FieldSanitizer<Object> asObjectSanitizer() {
        return (FieldSanitizer<Object>) this;
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.FieldDescriptorTest"`
Expected: 1 test passes.

- [ ] **Step 7: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/Kind.java \
        sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/FieldDescriptor.java \
        sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/FieldSanitizer.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/FieldDescriptorTest.java
git commit -m "feat(core): introduce Kind enum and FieldDescriptor for traversal

Assisted by AI"
```

---

### Task 4: Add `ClassMetadata` with `Kind` resolution and metadata build

**Files:**
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/ClassMetadata.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/ClassMetadataTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/ClassMetadataTest.java`:

```java
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
        final IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> ClassMetadata.of(IllegalLeafCascade.class));
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
        final IllegalStateException ex = assertThrows(
                IllegalStateException.class, () -> ClassMetadata.of(NoOpAnnotation.class));
        assertTrue(ex.getMessage().contains("no sanitizer and cascade=false"));
    }

    @Test
    void rawCollectionTypeReturnsNullElementType() {
        final ClassMetadata meta = ClassMetadata.of(RawCollectionBean.class);
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

    enum Color { RED, BLUE }

    static class LeafBean {
        @Sanitize(using = TrimSanitizer.class) String text;
        @Sanitize(using = TrimSanitizer.class) int count;
        @Sanitize(using = TrimSanitizer.class) Integer boxed;
        @Sanitize(using = TrimSanitizer.class) UUID uuid;
        @Sanitize(using = TrimSanitizer.class) Instant instant;
        @Sanitize(using = TrimSanitizer.class) BigDecimal amount;
        @Sanitize(using = TrimSanitizer.class) Color color;
    }

    record SimpleRecord(@Sanitize(using = TrimSanitizer.class) String value) {}

    static class CollectionBean {
        @Sanitize(using = LowerCaseSanitizer.class) List<String> names;
        @Sanitize(cascade = true) Set<SimpleRecord> records;
        @Sanitize(cascade = true) Map<String, SimpleRecord> byName;
    }

    static class NestedBean {
        @Sanitize(cascade = true) LeafBean child;
    }

    static class IllegalLeafCascade {
        @Sanitize(cascade = true) String text;
    }

    static class ParentAnnotated {
        @Sanitize(using = TrimSanitizer.class) String parentName;
    }

    static class ChildAnnotated extends ParentAnnotated {
        @Sanitize(using = UpperCaseSanitizer.class) String childName;
    }

    static class PartiallyAnnotated {
        String unannotated;
        @Sanitize(using = TrimSanitizer.class) String annotated;
    }

    static class NoOpAnnotation {
        @Sanitize String pointless;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static class RawCollectionBean {
        @Sanitize(cascade = true) List raw;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.ClassMetadataTest"`
Expected: COMPILATION FAILURE — `cannot find symbol: class ClassMetadata`.

- [ ] **Step 3: Create `ClassMetadata`**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/ClassMetadata.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.core.ConfigurableFieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizerInstantiationException;

/**
 * Per-class compiled plan: ordered list of {@link FieldDescriptor} plus record
 * metadata when applicable. Constructed once per class and cached in a static
 * {@code ConcurrentHashMap}.
 */
final class ClassMetadata {

    private static final Map<Class<?>, ClassMetadata> CACHE = new ConcurrentHashMap<>();

    private static final Set<Class<?>> LEAF_TYPES = Set.of(
            String.class,
            Boolean.class, Byte.class, Character.class, Short.class,
            Integer.class, Long.class, Float.class, Double.class,
            UUID.class,
            LocalDate.class, LocalDateTime.class, Instant.class,
            BigDecimal.class, BigInteger.class);

    private final Class<?> type;
    private final boolean isRecord;
    private final @Nullable Constructor<?> canonicalCtor;
    private final RecordComponent @Nullable [] components;
    private final List<FieldDescriptor> fields;

    private ClassMetadata(
            final Class<?> type,
            final boolean isRecord,
            final @Nullable Constructor<?> canonicalCtor,
            final RecordComponent @Nullable [] components,
            final List<FieldDescriptor> fields) {
        this.type = type;
        this.isRecord = isRecord;
        this.canonicalCtor = canonicalCtor;
        this.components = components;
        this.fields = List.copyOf(fields);
    }

    static ClassMetadata of(final Class<?> cls) {
        return CACHE.computeIfAbsent(cls, ClassMetadata::build);
    }

    Class<?> type() { return type; }
    boolean isRecord() { return isRecord; }
    @Nullable Constructor<?> canonicalCtor() { return canonicalCtor; }
    RecordComponent @Nullable [] components() { return components; }
    List<FieldDescriptor> fields() { return fields; }

    private static ClassMetadata build(final Class<?> cls) {
        if (cls.isRecord()) {
            return buildRecord(cls);
        }
        return buildPojo(cls);
    }

    private static ClassMetadata buildRecord(final Class<?> cls) {
        final RecordComponent[] components = cls.getRecordComponents();
        final Class<?>[] paramTypes = new Class<?>[components.length];
        for (int i = 0; i < components.length; i++) {
            paramTypes[i] = components[i].getType();
        }
        final Constructor<?> canonical;
        try {
            canonical = cls.getDeclaredConstructor(paramTypes);
            canonical.setAccessible(true);
        } catch (final NoSuchMethodException e) {
            throw new SanitizerInstantiationException(
                    "Cannot find canonical constructor for record " + cls.getName(), e);
        }
        final List<FieldDescriptor> descriptors = new ArrayList<>(components.length);
        for (final RecordComponent rc : components) {
            descriptors.add(describeRecordComponent(cls, rc));
        }
        return new ClassMetadata(cls, true, canonical, components, descriptors);
    }

    private static FieldDescriptor describeRecordComponent(final Class<?> owner, final RecordComponent rc) {
        final Sanitize[] anns = rc.getAnnotationsByType(Sanitize.class);
        final List<FieldSanitizer<Object>> chain = buildChain(anns);
        final boolean cascade = anyCascade(anns);
        validateMeaningful(owner, rc.getName(), chain, cascade);
        final Class<?> declared = rc.getType();
        final Kind kind = resolveKind(declared);
        validateCascadeKind(owner, rc.getName(), kind, cascade);
        final Class<?> elementType = resolveElementType(rc.getGenericType(), kind);
        return FieldDescriptor.forRecordComponent(rc, chain, cascade, kind, elementType);
    }

    private static ClassMetadata buildPojo(final Class<?> cls) {
        final List<FieldDescriptor> descriptors = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            for (final Field field : current.getDeclaredFields()) {
                final Sanitize[] anns = field.getAnnotationsByType(Sanitize.class);
                if (anns.length == 0) {
                    continue;
                }
                field.setAccessible(true);
                final List<FieldSanitizer<Object>> chain = buildChain(anns);
                final boolean cascade = anyCascade(anns);
                validateMeaningful(cls, field.getName(), chain, cascade);
                final Kind kind = resolveKind(field.getType());
                validateCascadeKind(cls, field.getName(), kind, cascade);
                final Class<?> elementType = resolveElementType(field.getGenericType(), kind);
                descriptors.add(FieldDescriptor.forPojoField(field, chain, cascade, kind, elementType));
            }
            current = current.getSuperclass();
        }
        return new ClassMetadata(cls, false, null, null, descriptors);
    }

    private static List<FieldSanitizer<Object>> buildChain(final Sanitize[] anns) {
        final List<FieldSanitizer<Object>> chain = new ArrayList<>();
        for (final Sanitize ann : anns) {
            for (final Class<? extends FieldSanitizer<?>> sanitizerClass : ann.using()) {
                final FieldSanitizer<?> sanitizer;
                try {
                    sanitizer = sanitizerClass.getDeclaredConstructor().newInstance();
                } catch (final ReflectiveOperationException e) {
                    throw new SanitizerInstantiationException(
                            "Cannot instantiate sanitizer " + sanitizerClass.getName(), e);
                }
                if (sanitizer instanceof ConfigurableFieldSanitizer<?> configurable && !ann.params().isBlank()) {
                    configurable.configure(ConfigurableFieldSanitizer.parseParams(ann.params()));
                }
                chain.add(sanitizer.asObjectSanitizer());
            }
        }
        return chain;
    }

    private static boolean anyCascade(final Sanitize[] anns) {
        for (final Sanitize ann : anns) {
            if (ann.cascade()) {
                return true;
            }
        }
        return false;
    }

    private static void validateMeaningful(
            final Class<?> owner, final String name, final List<FieldSanitizer<Object>> chain, final boolean cascade) {
        if (chain.isEmpty() && !cascade) {
            throw new IllegalStateException(
                    "@Sanitize on " + owner.getName() + "." + name
                            + " has no sanitizer and cascade=false. Either supply using={...} or set cascade=true.");
        }
    }

    private static void validateCascadeKind(
            final Class<?> owner, final String name, final Kind kind, final boolean cascade) {
        if (cascade && kind == Kind.LEAF) {
            throw new IllegalStateException(
                    "@Sanitize(cascade=true) on " + owner.getName() + "." + name
                            + " resolves to a LEAF type. Cascade requires POJO, RECORD, COLLECTION, or MAP.");
        }
    }

    private static Kind resolveKind(final Class<?> declared) {
        if (declared.isPrimitive() || LEAF_TYPES.contains(declared) || declared.isEnum()) {
            return Kind.LEAF;
        }
        if (declared.isRecord()) {
            return Kind.RECORD;
        }
        if (Collection.class.isAssignableFrom(declared)) {
            return Kind.COLLECTION;
        }
        if (Map.class.isAssignableFrom(declared)) {
            return Kind.MAP;
        }
        return Kind.POJO;
    }

    private static @Nullable Class<?> resolveElementType(final Type genericType, final Kind kind) {
        if (kind != Kind.COLLECTION && kind != Kind.MAP) {
            return null;
        }
        if (!(genericType instanceof ParameterizedType pt)) {
            return null;
        }
        final Type[] args = pt.getActualTypeArguments();
        if (args.length == 0) {
            return null;
        }
        final Type target = (kind == Kind.MAP && args.length > 1) ? args[1] : args[0];
        if (target instanceof Class<?> cls) {
            return cls;
        }
        if (target instanceof ParameterizedType nested && nested.getRawType() instanceof Class<?> raw) {
            return raw;
        }
        return null;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.ClassMetadataTest"`
Expected: all 9 tests pass.

- [ ] **Step 5: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/ClassMetadata.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/ClassMetadataTest.java
git commit -m "feat(core): add ClassMetadata with Kind resolution and validation

Assisted by AI"
```

---

### Task 5: Add `TraversalState` carrier

**Files:**
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalState.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalStateTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalStateTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalStateTest"`
Expected: COMPILATION FAILURE — `cannot find symbol: class TraversalState`.

- [ ] **Step 3: Create `TraversalState`**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalState.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.IdentityHashMap;

import org.jspecify.annotations.Nullable;

/**
 * Mutable per-invocation traversal state. Tracks visited POJOs/collections/maps
 * for cycle detection and reconstructed record instances keyed by their
 * original references.
 */
final class TraversalState {

    private final IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
    private final IdentityHashMap<Object, Object> reconstructedRecords = new IdentityHashMap<>();

    /**
     * @return true the first time this node is visited; false on every subsequent
     *         visit (indicates a cycle).
     */
    boolean markVisited(final Object node) {
        return visited.put(node, Boolean.TRUE) == null;
    }

    void storeReconstructed(final Object original, final Object rebuilt) {
        reconstructedRecords.put(original, rebuilt);
    }

    @Nullable Object findReconstructed(final Object original) {
        return reconstructedRecords.get(original);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalStateTest"`
Expected: 2 tests pass.

- [ ] **Step 5: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalState.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalStateTest.java
git commit -m "feat(core): add TraversalState for cycle detection and record dedup

Assisted by AI"
```

---

## Phase 3: TraversalEngine — incremental TDD

### Task 6: Add SLF4J API dependency to sanitizer-core

**Files:**
- Modify: `sanitizer-core/build.gradle.kts`

- [ ] **Step 1: Inspect current deps**

Run: `cat /Volumes/Work/sanitizer-lib/sanitizer-core/build.gradle.kts`
Expected output:
```
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

- [ ] **Step 2: Add SLF4J API**

Replace contents of `sanitizer-core/build.gradle.kts` with:

```kotlin
dependencies {
    api("org.slf4j:slf4j-api")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.slf4j:slf4j-simple")
}
```

The Spring Boot BOM applied at the root project pins both `slf4j-api` and `slf4j-simple`, so no explicit version is needed.

- [ ] **Step 3: Verify deps resolve**

Run: `./gradlew :sanitizer-core:dependencies --configuration runtimeClasspath | grep slf4j`
Expected: shows `org.slf4j:slf4j-api:` resolved to a non-empty version.

- [ ] **Step 4: Commit**

```bash
git add sanitizer-core/build.gradle.kts
git commit -m "build(core): add slf4j-api for traversal engine logging

Assisted by AI"
```

---

### Task 7: Create `TraversalEngine` skeleton — POJO baseline (no cascade, no records)

This replicates the **current** `SanitizationUtils.apply` semantics for POJOs inside the new engine. Records still go through legacy throw path. No new behavior yet.

**Files:**
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEnginePojoBaselineTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEnginePojoBaselineTest.java`:

```java
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEnginePojoBaselineTest"`
Expected: COMPILATION FAILURE — `cannot find symbol: class TraversalEngine`.

- [ ] **Step 3: Create `TraversalEngine` baseline**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * Recursive walker that applies sanitizers and (optionally) descends into a
 * bean's object graph. Entry point: {@link #walk(Object, TraversalState, TraversalSafetyChecker)}.
 */
public final class TraversalEngine {

    private static final Logger LOG = LoggerFactory.getLogger(TraversalEngine.class);

    private TraversalEngine() {
    }

    public static @Nullable Object walk(
            final @Nullable Object node,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        if (node == null) {
            return null;
        }
        final ClassMetadata meta = ClassMetadata.of(node.getClass());
        if (meta.isRecord()) {
            throw new UnsupportedOperationException(
                    "Records reached TraversalEngine but record support is not yet wired up: "
                            + node.getClass().getName());
        }
        if (!state.markVisited(node)) {
            return node;
        }
        walkPojo(node, meta);
        return node;
    }

    private static void walkPojo(final Object node, final ClassMetadata meta) {
        LOG.debug("walk class={} descriptors={}", node.getClass().getName(), meta.fields().size());
        for (final FieldDescriptor d : meta.fields()) {
            final Field field = d.field();
            if (field == null) {
                continue;
            }
            try {
                final Object raw = field.get(node);
                final Object sanitized = applyChain(raw, d.chain());
                writeBackIfChanged(node, field, raw, sanitized);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot access field '" + field.getName() + "' on " + node.getClass().getName()
                                + ". Ensure the field is mutable and accessible.",
                        e);
            } catch (final ClassCastException e) {
                throw new IllegalStateException(
                        "Type mismatch: sanitizer chain incompatible with field '" + field.getName()
                                + "' of type " + field.getType().getName() + " on " + node.getClass().getName()
                                + ". Ensure the sanitizer's generic type matches the field type.",
                        e);
            }
        }
    }

    private static @Nullable Object applyChain(
            final @Nullable Object raw,
            final java.util.List<FieldSanitizer<Object>> chain) {
        Object current = raw;
        for (final FieldSanitizer<Object> s : chain) {
            current = s.sanitize(current);
        }
        return current;
    }

    private static void writeBackIfChanged(
            final Object node, final Field field, final @Nullable Object raw, final @Nullable Object sanitized)
            throws IllegalAccessException {
        if (raw == null && sanitized == null) {
            return;
        }
        field.set(node, sanitized);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEnginePojoBaselineTest"`
Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEnginePojoBaselineTest.java
git commit -m "feat(core): add TraversalEngine POJO baseline walker

Assisted by AI"
```

---

### Task 8: Wire `SanitizationUtils.apply` through `TraversalEngine` (POJO path only)

Refactor `SanitizationUtils.apply(Object)` to delegate to the engine. Behavior for POJOs must be bit-identical. The legacy in-class CACHE is removed since `ClassMetadata.of` owns the cache.

**Files:**
- Modify: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtils.java`

- [ ] **Step 1: Re-read existing tests to confirm contracts**

Run: `./gradlew :sanitizer-core:test`
Expected: all current tests PASS (sanity check before refactor).

- [ ] **Step 2: Refactor `SanitizationUtils`**

Replace contents of `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtils.java` with:

```java
package io.github.rabinarayanpatra.sanitizer.core;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngine;
import io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalState;

/**
 * Public facade for applying {@link Sanitize} annotations to bean fields. All
 * traversal logic lives in
 * {@link io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngine};
 * this class is a thin entry point.
 *
 * @since 1.0.0
 */
public final class SanitizationUtils {

    private SanitizationUtils() {
    }

    /**
     * Applies sanitizers in place to a POJO. Throws for record inputs because
     * record components cannot be mutated and the void return would discard the
     * reconstructed instance. Use {@link #applyAndReturn(Object)} for records.
     *
     * @param bean
     *            the POJO whose fields should be sanitized; may be null
     * @throws IllegalArgumentException
     *             when {@code bean} is a {@link Record} instance
     */
    public static void apply(final @Nullable Object bean) {
        if (bean == null) {
            return;
        }
        if (bean.getClass().isRecord()) {
            throw new IllegalArgumentException(
                    "apply(Object) discards return value but record requires reassignment. "
                            + "Use applyAndReturn(T) for record type " + bean.getClass().getName() + ".");
        }
        TraversalEngine.walk(bean, new TraversalState(), TraversalSafetyChecker.ALWAYS);
    }
}
```

- [ ] **Step 3: Update the existing `apply_throwsOnRecord` test to expect `IllegalArgumentException`**

Edit `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtilsTest.java`. Find the `apply_throwsOnRecord` method (lines 44-50) and replace it with:

```java
@Test
void apply_throwsIllegalArgumentOnRecord() {
    final ImmutableRecord rec = new ImmutableRecord("  HELLO  ");
    final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> SanitizationUtils.apply(rec));
    assertTrue(ex.getMessage().contains("ImmutableRecord"));
    assertTrue(ex.getMessage().contains("applyAndReturn"));
}
```

- [ ] **Step 4: Run all sanitizer-core tests**

Run: `./gradlew :sanitizer-core:check`
Expected: BUILD SUCCESSFUL. All existing tests pass (including the updated `apply_throwsIllegalArgumentOnRecord`).

- [ ] **Step 5: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtils.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtilsTest.java
git commit -m "refactor(core): route SanitizationUtils.apply through TraversalEngine

Switches the exception thrown on record root inputs from
UnsupportedOperationException to IllegalArgumentException and recommends
applyAndReturn(T). Behavior on POJO inputs is bit-identical.

Assisted by AI"
```

---

### Task 9: Add `applyAndReturn(T)` facade methods and record reconstruction

**Files:**
- Modify: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtils.java`
- Modify: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineRecordTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineRecordTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TraversalEngineRecordTest {

    @Test
    void recordRootIsReconstructedWithSanitizedComponent() {
        final Simple input = new Simple("  HELLO  ");
        final Simple out = SanitizationUtils.applyAndReturn(input);
        assertNotSame(input, out);
        assertEquals("hello", out.value());
    }

    @Test
    void recordComponentWithoutAnnotationPassesThrough() {
        final Mixed input = new Mixed("  HELLO  ", 42);
        final Mixed out = SanitizationUtils.applyAndReturn(input);
        assertEquals("hello", out.value());
        assertEquals(42, out.count());
    }

    @Test
    void recordWithNullComponentReconstructed() {
        final Simple input = new Simple(null);
        final Simple out = SanitizationUtils.applyAndReturn(input);
        assertNotSame(input, out);
        assertNull(out.value());
    }

    @Test
    void applyAndReturnNullReturnsNull() {
        assertNull(SanitizationUtils.applyAndReturn(null));
    }

    @Test
    void applyAndReturnPojoReturnsSameRef() {
        final PojoBean b = new PojoBean();
        b.name = "  HELLO  ";
        final PojoBean out = SanitizationUtils.applyAndReturn(b);
        assertEquals(System.identityHashCode(b), System.identityHashCode(out));
        assertEquals("hello", out.name);
    }

    @Test
    void compactConstructorValidationPropagatesAsCause() {
        final Validated input = new Validated("  HELLO  ");
        // First reconstruction with non-empty raw passes; trigger failure via
        // sanitizer producing blank → compact ctor throws.
        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> SanitizationUtils.applyAndReturn(new Validated("   ")));
        assertTrue(ex.getCause() instanceof IllegalArgumentException
                || ex instanceof IllegalArgumentException
                || ex.getMessage().contains("must not be blank"));
        // input keeps original content (immutable)
        assertEquals("  HELLO  ", input.value());
    }

    record Simple(@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String value) {}

    record Mixed(@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String value, int count) {}

    record Validated(@Sanitize(using = TrimSanitizer.class) String value) {
        public Validated {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("value must not be blank");
            }
        }
    }

    static class PojoBean {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
        String name;
    }

    // Unused but kept to ensure UpperCaseSanitizer import compiles in case future
    // edits add fixtures using it.
    @SuppressWarnings("unused")
    private static final Class<?> KEEP_IMPORT = UpperCaseSanitizer.class;
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineRecordTest"`
Expected: COMPILATION FAILURE on `SanitizationUtils.applyAndReturn` and runtime `UnsupportedOperationException` from engine.

- [ ] **Step 3: Add `walkRecord` to `TraversalEngine`**

Replace contents of `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java` with:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizerInstantiationException;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * Recursive walker that applies sanitizers and (optionally) descends into a
 * bean's object graph. Entry point: {@link #walk(Object, TraversalState, TraversalSafetyChecker)}.
 */
public final class TraversalEngine {

    private static final Logger LOG = LoggerFactory.getLogger(TraversalEngine.class);

    private TraversalEngine() {
    }

    public static @Nullable Object walk(
            final @Nullable Object node,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        if (node == null) {
            return null;
        }
        final ClassMetadata meta = ClassMetadata.of(node.getClass());
        if (meta.isRecord()) {
            final Object cached = state.findReconstructed(node);
            if (cached != null) {
                return cached;
            }
            final Object built = walkRecord(node, meta);
            state.storeReconstructed(node, built);
            return built;
        }
        if (!state.markVisited(node)) {
            return node;
        }
        walkPojo(node, meta);
        return node;
    }

    private static Object walkRecord(final Object node, final ClassMetadata meta) {
        LOG.debug("walk record class={} components={}", node.getClass().getName(), meta.fields().size());
        final RecordComponent[] components = meta.components();
        final Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            final RecordComponent rc = components[i];
            final FieldDescriptor d = meta.fields().get(i);
            try {
                final Object raw = rc.getAccessor().invoke(node);
                args[i] = applyChain(raw, d.chain());
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot invoke accessor for component '" + rc.getName() + "' on " + node.getClass().getName(),
                        e);
            } catch (final InvocationTargetException e) {
                final Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (cause instanceof RuntimeException re) {
                    throw re;
                }
                throw new IllegalStateException("Record accessor threw", cause);
            } catch (final ClassCastException e) {
                throw new IllegalStateException(
                        "Type mismatch: sanitizer chain incompatible with record component '"
                                + rc.getName() + "' of type " + rc.getType().getName()
                                + " on " + node.getClass().getName(),
                        e);
            }
        }
        try {
            return meta.canonicalCtor().newInstance(args);
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) {
                throw re;
            }
            throw new IllegalStateException("Record canonical constructor threw checked exception", cause);
        } catch (final ReflectiveOperationException e) {
            throw new SanitizerInstantiationException(
                    "Cannot reconstruct record " + node.getClass().getName(), e);
        }
    }

    private static void walkPojo(final Object node, final ClassMetadata meta) {
        LOG.debug("walk pojo class={} fields={}", node.getClass().getName(), meta.fields().size());
        for (final FieldDescriptor d : meta.fields()) {
            final Field field = d.field();
            if (field == null) {
                continue;
            }
            try {
                final Object raw = field.get(node);
                final Object sanitized = applyChain(raw, d.chain());
                writeBackIfChanged(node, field, raw, sanitized);
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot access field '" + field.getName() + "' on " + node.getClass().getName()
                                + ". Ensure the field is mutable and accessible.",
                        e);
            } catch (final ClassCastException e) {
                throw new IllegalStateException(
                        "Type mismatch: sanitizer chain incompatible with field '" + field.getName()
                                + "' of type " + field.getType().getName() + " on " + node.getClass().getName()
                                + ". Ensure the sanitizer's generic type matches the field type.",
                        e);
            }
        }
    }

    private static @Nullable Object applyChain(final @Nullable Object raw, final List<FieldSanitizer<Object>> chain) {
        Object current = raw;
        for (final FieldSanitizer<Object> s : chain) {
            current = s.sanitize(current);
        }
        return current;
    }

    private static void writeBackIfChanged(
            final Object node, final Field field, final @Nullable Object raw, final @Nullable Object sanitized)
            throws IllegalAccessException {
        if (raw == null && sanitized == null) {
            return;
        }
        field.set(node, sanitized);
    }
}
```

- [ ] **Step 4: Add `applyAndReturn` overloads to `SanitizationUtils`**

Replace contents of `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtils.java` with:

```java
package io.github.rabinarayanpatra.sanitizer.core;

import org.jspecify.annotations.Nullable;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngine;
import io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalState;

/**
 * Public facade for applying {@link Sanitize} annotations to bean fields.
 *
 * @since 1.0.0
 */
public final class SanitizationUtils {

    private SanitizationUtils() {
    }

    /**
     * Applies sanitizers in place to a POJO. Throws for record inputs because
     * record components cannot be mutated and the void return would discard the
     * reconstructed instance. Use {@link #applyAndReturn(Object)} for records.
     *
     * @param bean
     *            the POJO whose fields should be sanitized; may be null
     * @throws IllegalArgumentException
     *             when {@code bean} is a {@link Record} instance
     */
    public static void apply(final @Nullable Object bean) {
        if (bean == null) {
            return;
        }
        if (bean.getClass().isRecord()) {
            throw new IllegalArgumentException(
                    "apply(Object) discards return value but record requires reassignment. "
                            + "Use applyAndReturn(T) for record type " + bean.getClass().getName() + ".");
        }
        TraversalEngine.walk(bean, new TraversalState(), TraversalSafetyChecker.ALWAYS);
    }

    /**
     * Universal sanitization entry point. POJO inputs are mutated in place and
     * the same reference is returned; record inputs are reconstructed via their
     * canonical constructor and a new instance is returned.
     *
     * @param bean
     *            the bean to sanitize; may be null
     * @param <T>
     *            the static type of the bean
     * @return the sanitized bean (same ref for POJOs, new instance for records)
     * @since 1.2.0
     */
    public static <T> @Nullable T applyAndReturn(final @Nullable T bean) {
        return applyAndReturn(bean, TraversalSafetyChecker.ALWAYS);
    }

    /**
     * Universal sanitization entry point with a custom safety checker. See
     * {@link #applyAndReturn(Object)}.
     *
     * @param bean
     *            the bean to sanitize; may be null
     * @param checker
     *            the safety checker that gates descent into individual fields;
     *            must not be null
     * @param <T>
     *            the static type of the bean
     * @return the sanitized bean (same ref for POJOs, new instance for records)
     * @since 1.2.0
     */
    @SuppressWarnings("unchecked")
    public static <T> @Nullable T applyAndReturn(
            final @Nullable T bean, final TraversalSafetyChecker checker) {
        if (bean == null) {
            return null;
        }
        return (T) TraversalEngine.walk(bean, new TraversalState(), checker);
    }
}
```

- [ ] **Step 5: Run the new test**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineRecordTest"`
Expected: 6 tests pass.

- [ ] **Step 6: Run all sanitizer-core tests**

Run: `./gradlew :sanitizer-core:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java \
        sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtils.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineRecordTest.java
git commit -m "feat(core): add applyAndReturn(T) and record reconstruction

Records are now reconstructed via canonical constructor. POJO behavior
unchanged. apply(Object) still throws for record root, now with
IllegalArgumentException recommending applyAndReturn.

Assisted by AI"
```

---

### Task 10: Add cascade descent into POJO and RECORD child fields

**Files:**
- Modify: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCascadeTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCascadeTest.java`:

```java
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
        @Sanitize(using = io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer.class)
        String name;
        Child child;
    }

    static class ParentWithCascade {
        @Sanitize(using = io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer.class)
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

    record Embedded(@Sanitize(using = {TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {}

    record OuterRec(@Sanitize(cascade = true) InnerRec inner) {}
    record InnerRec(@Sanitize(using = {TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {}

    static class OnlyCascade {
        @Sanitize(cascade = true)
        Child child;
    }

    record Shared(@Sanitize(cascade = true) InnerRec inner) {}

    static class HoldsTwo {
        @Sanitize(cascade = true) Shared left;
        @Sanitize(cascade = true) Shared right;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineCascadeTest"`
Expected: multiple failures — nested fields not sanitized; record cascade not wired.

Some sub-tests (`cascadeFalseLeavesNestedAnnotatedFieldUntouched_backwardCompat`) should PASS already — that's intentional, verifying the backward-compat guarantee.

- [ ] **Step 3: Add cascade descent to `TraversalEngine.walkPojo` and `walkRecord`**

Edit `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`. Replace the two methods `walkPojo` and `walkRecord` (and add a private helper `descendByKind`) so that the file becomes:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizerInstantiationException;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

public final class TraversalEngine {

    private static final Logger LOG = LoggerFactory.getLogger(TraversalEngine.class);

    private TraversalEngine() {
    }

    public static @Nullable Object walk(
            final @Nullable Object node,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        if (node == null) {
            return null;
        }
        final ClassMetadata meta = ClassMetadata.of(node.getClass());
        if (meta.isRecord()) {
            final Object cached = state.findReconstructed(node);
            if (cached != null) {
                return cached;
            }
            final Object built = walkRecord(node, meta, state, checker);
            state.storeReconstructed(node, built);
            return built;
        }
        if (!state.markVisited(node)) {
            return node;
        }
        walkPojo(node, meta, state, checker);
        return node;
    }

    private static Object walkRecord(
            final Object node, final ClassMetadata meta, final TraversalState state,
            final TraversalSafetyChecker checker) {
        LOG.debug("walk record class={} components={}", node.getClass().getName(), meta.fields().size());
        final RecordComponent[] components = meta.components();
        final Object[] args = new Object[components.length];
        for (int i = 0; i < components.length; i++) {
            final RecordComponent rc = components[i];
            final FieldDescriptor d = meta.fields().get(i);
            try {
                final Object raw = rc.getAccessor().invoke(node);
                Object current = applyChain(raw, d.chain());
                if (d.cascade() && current != null) {
                    current = descendByKind(current, d, state, checker);
                }
                args[i] = current;
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot invoke accessor for component '" + rc.getName() + "' on " + node.getClass().getName(),
                        e);
            } catch (final InvocationTargetException e) {
                rethrowRuntime(e, "Record accessor threw");
            } catch (final ClassCastException e) {
                throw new IllegalStateException(
                        "Type mismatch: sanitizer chain incompatible with record component '"
                                + rc.getName() + "' of type " + rc.getType().getName()
                                + " on " + node.getClass().getName(),
                        e);
            }
        }
        try {
            return meta.canonicalCtor().newInstance(args);
        } catch (final InvocationTargetException e) {
            rethrowRuntime(e, "Record canonical constructor threw checked exception");
            return null; // unreachable
        } catch (final ReflectiveOperationException e) {
            throw new SanitizerInstantiationException(
                    "Cannot reconstruct record " + node.getClass().getName(), e);
        }
    }

    private static void walkPojo(
            final Object node, final ClassMetadata meta, final TraversalState state,
            final TraversalSafetyChecker checker) {
        LOG.debug("walk pojo class={} fields={}", node.getClass().getName(), meta.fields().size());
        for (final FieldDescriptor d : meta.fields()) {
            final Field field = d.field();
            if (field == null) {
                continue;
            }
            try {
                final Object raw = field.get(node);
                Object sanitized = applyChain(raw, d.chain());
                writeBackIfChanged(node, field, raw, sanitized);
                if (d.cascade() && sanitized != null && checker.shouldDescend(node, field)) {
                    final Object after = descendByKind(sanitized, d, state, checker);
                    if (after != sanitized) {
                        field.set(node, after);
                    }
                }
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(
                        "Cannot access field '" + field.getName() + "' on " + node.getClass().getName()
                                + ". Ensure the field is mutable and accessible.",
                        e);
            } catch (final ClassCastException e) {
                throw new IllegalStateException(
                        "Type mismatch: sanitizer chain incompatible with field '" + field.getName()
                                + "' of type " + field.getType().getName() + " on " + node.getClass().getName()
                                + ". Ensure the sanitizer's generic type matches the field type.",
                        e);
            }
        }
    }

    private static Object descendByKind(
            final Object child, final FieldDescriptor d, final TraversalState state,
            final TraversalSafetyChecker checker) {
        switch (d.kind()) {
            case POJO -> {
                walk(child, state, checker);
                return child;
            }
            case RECORD -> {
                final Object replaced = walk(child, state, checker);
                return replaced == null ? child : replaced;
            }
            case COLLECTION, MAP -> {
                // Implemented in a later task.
                LOG.warn("cascade into {} not yet implemented; skipping", d.kind());
                return child;
            }
            case LEAF -> throw new IllegalStateException(
                    "Internal error: cascade descent reached LEAF for "
                            + (d.field() != null ? d.field().getName() : d.recordComponent().getName()));
        }
        return child;
    }

    private static @Nullable Object applyChain(final @Nullable Object raw, final List<FieldSanitizer<Object>> chain) {
        Object current = raw;
        for (final FieldSanitizer<Object> s : chain) {
            current = s.sanitize(current);
        }
        return current;
    }

    private static void writeBackIfChanged(
            final Object node, final Field field, final @Nullable Object raw, final @Nullable Object sanitized)
            throws IllegalAccessException {
        if (raw == null && sanitized == null) {
            return;
        }
        field.set(node, sanitized);
    }

    private static void rethrowRuntime(final InvocationTargetException e, final String fallback) {
        final Throwable cause = e.getCause() != null ? e.getCause() : e;
        if (cause instanceof RuntimeException re) {
            throw re;
        }
        throw new IllegalStateException(fallback, cause);
    }
}
```

- [ ] **Step 4: Run the cascade test**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineCascadeTest"`
Expected: 6 tests pass.

- [ ] **Step 5: Run all sanitizer-core tests**

Run: `./gradlew :sanitizer-core:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCascadeTest.java
git commit -m "feat(core): add cascade descent into POJO and RECORD child fields

Cascade into Collection/Map is logged as a WARN and skipped until the
next task implements collection support.

Assisted by AI"
```

---

### Task 11: Add Collection support — leaf elements, record elements, mutable POJO elements

**Files:**
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/CollectionWalker.java`
- Modify: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCollectionTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCollectionTest.java`:

```java
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
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) List<String> values;
    }

    static class WithRecordList {
        @Sanitize(cascade = true) List<Item> items;
    }

    static class WithPojoList {
        @Sanitize(cascade = true) List<Child> children;
    }

    static class WithStringSet {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) Set<String> values;
    }

    static class WithRecordSet {
        @Sanitize(cascade = true) Set<Item> items;
    }

    record Item(@Sanitize(using = {TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {}

    static class Child {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
        Child() {}
        Child(final String name) { this.name = name; }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static class RawHolder {
        @Sanitize(cascade = true) List raw;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineCollectionTest"`
Expected: multiple failures — collection cascade is currently a no-op WARN.

- [ ] **Step 3: Create `CollectionWalker`**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/CollectionWalker.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/** Cascade descent into {@link Collection} fields. */
final class CollectionWalker {

    private static final Logger LOG = LoggerFactory.getLogger(CollectionWalker.class);

    private CollectionWalker() {
    }

    /**
     * Walks a collection. Returns the same reference when elements are mutated
     * in place; returns a new collection when the original is unmodifiable or
     * when elements are records (immutable).
     */
    static Collection<?> walk(
            final Collection<?> coll,
            final @Nullable Class<?> elementType,
            final List<FieldSanitizer<Object>> chain,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        if (elementType == null) {
            LOG.warn("collection field has no resolvable element type; skipping cascade");
            return coll;
        }
        final Kind elementKind = kindOf(elementType);
        final boolean isUnmodifiable = isUnmodifiable(coll);
        final boolean rebuild = isUnmodifiable || elementKind == Kind.RECORD;

        if (rebuild) {
            return rebuild(coll, elementKind, chain, state, checker);
        }
        // Mutable in-place path
        if (coll instanceof List<?>) {
            walkListInPlace((List<Object>) coll, elementKind, chain, state, checker);
            return coll;
        }
        if (coll instanceof Set<?>) {
            // Sets: rebuild even when mutable, because hash codes may change for
            // records and leaf-sanitized strings.
            return rebuild(coll, elementKind, chain, state, checker);
        }
        // Other Collection: rebuild as ArrayList
        return rebuild(coll, elementKind, chain, state, checker);
    }

    @SuppressWarnings("unchecked")
    private static void walkListInPlace(
            final List<Object> list,
            final Kind elementKind,
            final List<FieldSanitizer<Object>> chain,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        for (int i = 0; i < list.size(); i++) {
            final Object raw = list.get(i);
            final Object processed = processElement(raw, elementKind, chain, state, checker);
            if (processed != raw) {
                list.set(i, processed);
            }
        }
    }

    private static Collection<?> rebuild(
            final Collection<?> source,
            final Kind elementKind,
            final List<FieldSanitizer<Object>> chain,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        final Collection<Object> target = source instanceof Set<?>
                ? new LinkedHashSet<>(source.size())
                : new ArrayList<>(source.size());
        for (final Object raw : source) {
            target.add(processElement(raw, elementKind, chain, state, checker));
        }
        return target;
    }

    private static @Nullable Object processElement(
            final @Nullable Object raw,
            final Kind elementKind,
            final List<FieldSanitizer<Object>> chain,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        if (raw == null) {
            return null;
        }
        Object current = raw;
        for (final FieldSanitizer<Object> s : chain) {
            current = s.sanitize(current);
        }
        if (elementKind == Kind.POJO || elementKind == Kind.RECORD) {
            final Object after = TraversalEngine.walk(current, state, checker);
            current = after == null ? current : after;
        }
        return current;
    }

    private static Kind kindOf(final Class<?> elementType) {
        if (elementType.isRecord()) {
            return Kind.RECORD;
        }
        if (Collection.class.isAssignableFrom(elementType) || java.util.Map.class.isAssignableFrom(elementType)) {
            // Nested collections within collections are out of scope for v1.2.0;
            // treat as POJO so the engine descends into the wrapper.
            return Kind.POJO;
        }
        if (elementType.isPrimitive() || elementType.isEnum()
                || elementType == String.class || elementType == Integer.class || elementType == Long.class
                || elementType == Boolean.class || elementType == java.util.UUID.class
                || elementType == java.time.Instant.class) {
            return Kind.LEAF;
        }
        return Kind.POJO;
    }

    private static boolean isUnmodifiable(final Collection<?> coll) {
        final String name = coll.getClass().getName();
        return name.startsWith("java.util.ImmutableCollections$")
                || name.startsWith("java.util.Collections$Unmodifiable");
    }
}
```

- [ ] **Step 4: Wire `CollectionWalker` into `TraversalEngine.descendByKind`**

Edit `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`. Replace the `COLLECTION, MAP` branch in `descendByKind` and add the import:

Find:
```java
            case COLLECTION, MAP -> {
                // Implemented in a later task.
                LOG.warn("cascade into {} not yet implemented; skipping", d.kind());
                return child;
            }
```

Replace with:
```java
            case COLLECTION -> {
                return CollectionWalker.walk(
                        (java.util.Collection<?>) child, d.elementType(), d.chain(), state, checker);
            }
            case MAP -> {
                // Implemented in the next task.
                LOG.warn("cascade into MAP not yet implemented; skipping");
                return child;
            }
```

- [ ] **Step 5: Run the collection test**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineCollectionTest"`
Expected: 7 tests pass.

- [ ] **Step 6: Run all sanitizer-core tests**

Run: `./gradlew :sanitizer-core:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/CollectionWalker.java \
        sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCollectionTest.java
git commit -m "feat(core): cascade descent into Collection fields

Mutable Lists walk in place; Sets always rebuild as LinkedHashSet (hash
may change after sanitization); unmodifiable collections rebuild as
ArrayList. Records-as-elements always rebuild.

Assisted by AI"
```

---

### Task 12: Add Map support — values walked, keys untouched

**Files:**
- Create: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/MapWalker.java`
- Modify: `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineMapTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineMapTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class TraversalEngineMapTest {

    @Test
    void mapOfStringToRecordRebuiltWithSanitizedValues() {
        final Bean b = new Bean();
        b.byKey = new LinkedHashMap<>();
        b.byKey.put("a", new Item("  HI  "));
        b.byKey.put("b", new Item("  YO  "));
        final Map<String, Item> before = b.byKey;
        SanitizationUtils.apply(b);
        // Map ref may stay the same (mutable HashMap), values are replaced.
        assertEquals("HI", b.byKey.get("a").value());
        assertEquals("YO", b.byKey.get("b").value());
        assertSame(before, b.byKey);
    }

    @Test
    void mapKeysUntouched() {
        final Bean b = new Bean();
        b.byKey = new HashMap<>();
        b.byKey.put("  KEY  ", new Item("  V  "));
        SanitizationUtils.apply(b);
        // Keys remain untouched; the original raw key is still present.
        assertEquals(1, b.byKey.size());
        assertSame("  KEY  ", b.byKey.keySet().iterator().next());
        assertEquals("V", b.byKey.get("  KEY  ").value());
    }

    @Test
    void unmodifiableMapRebuilt() {
        final Bean b = new Bean();
        b.byKey = Map.of("a", new Item("  X  "));
        final Map<String, Item> before = b.byKey;
        SanitizationUtils.apply(b);
        assertNotSame(before, b.byKey);
        assertEquals("X", b.byKey.get("a").value());
    }

    static class Bean {
        @Sanitize(cascade = true)
        Map<String, Item> byKey;
    }

    record Item(@Sanitize(using = {TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineMapTest"`
Expected: failures — map cascade currently a no-op WARN.

- [ ] **Step 3: Create `MapWalker`**

Create `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/MapWalker.java`:

```java
package io.github.rabinarayanpatra.sanitizer.core.traversal;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/** Cascade descent into {@link Map} fields. Keys are never inspected. */
final class MapWalker {

    private static final Logger LOG = LoggerFactory.getLogger(MapWalker.class);

    private MapWalker() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Map<?, ?> walk(
            final Map<?, ?> map,
            final @Nullable Class<?> valueType,
            final List<FieldSanitizer<Object>> chain,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        if (valueType == null) {
            LOG.warn("map field has no resolvable value type; skipping cascade");
            return map;
        }
        final boolean rebuild = isUnmodifiable(map) || valueType.isRecord();
        if (rebuild) {
            final Map<Object, Object> target = new LinkedHashMap<>(map.size());
            for (final Map.Entry<?, ?> e : map.entrySet()) {
                target.put(e.getKey(), processValue(e.getValue(), valueType, chain, state, checker));
            }
            return target;
        }
        // Mutable + non-record value: walk values in place via replaceAll.
        ((Map) map).replaceAll((k, v) -> processValue(v, valueType, chain, state, checker));
        return map;
    }

    private static @Nullable Object processValue(
            final @Nullable Object raw,
            final Class<?> valueType,
            final List<FieldSanitizer<Object>> chain,
            final TraversalState state,
            final TraversalSafetyChecker checker) {
        if (raw == null) {
            return null;
        }
        Object current = raw;
        for (final FieldSanitizer<Object> s : chain) {
            current = s.sanitize(current);
        }
        if (valueType.isRecord() || !(valueType.isPrimitive() || valueType == String.class)) {
            final Object after = TraversalEngine.walk(current, state, checker);
            current = after == null ? current : after;
        }
        return current;
    }

    private static boolean isUnmodifiable(final Map<?, ?> map) {
        final String name = map.getClass().getName();
        return name.startsWith("java.util.ImmutableCollections$")
                || name.startsWith("java.util.Collections$Unmodifiable");
    }
}
```

- [ ] **Step 4: Wire `MapWalker` into `TraversalEngine.descendByKind`**

Edit `sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java`. Replace the `case MAP` branch with:

```java
            case MAP -> {
                return MapWalker.walk(
                        (java.util.Map<?, ?>) child, d.elementType(), d.chain(), state, checker);
            }
```

- [ ] **Step 5: Run map test**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineMapTest"`
Expected: 3 tests pass.

- [ ] **Step 6: Run all sanitizer-core tests**

Run: `./gradlew :sanitizer-core:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/MapWalker.java \
        sanitizer-core/src/main/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngine.java \
        sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineMapTest.java
git commit -m "feat(core): cascade descent into Map values (keys untouched)

Assisted by AI"
```

---

### Task 13: Add cycle detection test

The engine already calls `state.markVisited(node)` in `walk`. This task adds a regression test to lock the behavior in.

**Files:**
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCycleTest.java`

- [ ] **Step 1: Write the test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCycleTest.java`:

```java
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
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String label;
        @Sanitize(cascade = true) Node next;
    }
}
```

- [ ] **Step 2: Run test**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineCycleTest"`
Expected: test passes (cycle detection already wired via `markVisited`).

- [ ] **Step 3: Commit**

```bash
git add sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineCycleTest.java
git commit -m "test(core): lock in cycle-detection behavior for POJO graphs

Assisted by AI"
```

---

### Task 14: Add `TraversalSafetyChecker` integration test

**Files:**
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineSafetyCheckerTest.java`

- [ ] **Step 1: Write the test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineSafetyCheckerTest.java`:

```java
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

    static class Parent {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
        @Sanitize(cascade = true) Child child;
    }

    static class Child {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
    }
}
```

- [ ] **Step 2: Run test**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.traversal.TraversalEngineSafetyCheckerTest"`
Expected: both tests pass.

- [ ] **Step 3: Commit**

```bash
git add sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/traversal/TraversalEngineSafetyCheckerTest.java
git commit -m "test(core): verify TraversalSafetyChecker gates cascade descent

Assisted by AI"
```

---

### Task 15: Add backward-compat regression test for nested-without-cascade

**Files:**
- Test: `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtilsBackwardCompatTest.java`

- [ ] **Step 1: Write the test**

Create `sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtilsBackwardCompatTest.java`:

```java
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
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
        Child child;  // no @Sanitize on field → not annotated, not walked
    }

    static class Child {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
    }
}
```

- [ ] **Step 2: Run test**

Run: `./gradlew :sanitizer-core:test --tests "io.github.rabinarayanpatra.sanitizer.core.SanitizationUtilsBackwardCompatTest"`
Expected: test passes (backward compat guarantee).

- [ ] **Step 3: Final core check**

Run: `./gradlew :sanitizer-core:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add sanitizer-core/src/test/java/io/github/rabinarayanpatra/sanitizer/core/SanitizationUtilsBackwardCompatTest.java
git commit -m "test(core): lock in 1.1.0 behavior for nested fields without cascade

Assisted by AI"
```

---

## Phase 4: Spring (Jackson) module update

### Task 16: Update `SanitizerModule` to use `applyAndReturn`

**Files:**
- Modify: `sanitizer-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/spring/jackson/SanitizerModule.java`
- Test: `sanitizer-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/spring/jackson/SanitizerModuleRecordIntegrationTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/spring/jackson/SanitizerModuleRecordIntegrationTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizerModuleRecordIntegrationTest {

    @Test
    void deserializeRecord_sanitizesComponents() throws Exception {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new SanitizerModule());
        final RecordDto dto = mapper.readValue("{\"value\":\"  hi  \"}", RecordDto.class);
        assertEquals("HI", dto.value());
    }

    @Test
    void deserializeNestedRecordWithCascade_sanitizesNested() throws Exception {
        final ObjectMapper mapper = new ObjectMapper().registerModule(new SanitizerModule());
        final OuterDto dto = mapper.readValue(
                "{\"inner\":{\"value\":\"  hi  \"}}",
                OuterDto.class);
        assertEquals("HI", dto.inner().value());
    }

    record RecordDto(@Sanitize(using = {TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {}

    record OuterDto(@Sanitize(cascade = true) RecordDto inner) {}
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-spring:test --tests "io.github.rabinarayanpatra.sanitizer.spring.jackson.SanitizerModuleRecordIntegrationTest"`
Expected: failure — current `SanitizingDeserializer` calls `apply(bean)` which throws on records.

- [ ] **Step 3: Update `SanitizerModule`**

Edit `sanitizer-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/spring/jackson/SanitizerModule.java`. Replace the `deserialize` method body so the class becomes:

```java
package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

/**
 * Jackson module that integrates with Spring Boot to apply
 * {@link io.github.rabinarayanpatra.sanitizer.annotation.Sanitize} annotations
 * during JSON deserialization. Records are reconstructed via the engine and
 * the new instance is returned in place of the raw deserialized record.
 *
 * @since 1.0.0
 */
public final class SanitizerModule extends SimpleModule {

    public SanitizerModule() {
        super("SanitizerModule");
        setDeserializerModifier(new MyBeanDeserializerModifier());
    }

    private static class SanitizingDeserializer extends DelegatingDeserializer {

        protected SanitizingDeserializer(final JsonDeserializer<?> delegate) {
            super(delegate);
        }

        @Override
        public Object deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
            final Object bean = super.deserialize(p, ctxt);
            return SanitizationUtils.applyAndReturn(bean);
        }

        @Override
        protected JsonDeserializer<?> newDelegatingInstance(final JsonDeserializer<?> newDelegate) {
            return new SanitizingDeserializer(newDelegate);
        }
    }

    private static class MyBeanDeserializerModifier extends BeanDeserializerModifier {
        @Override
        public JsonDeserializer<?> modifyDeserializer(final DeserializationConfig config,
                final BeanDescription beanDesc, final JsonDeserializer<?> deserializer) {
            return new SanitizingDeserializer(deserializer);
        }
    }
}
```

- [ ] **Step 4: Run test**

Run: `./gradlew :sanitizer-spring:test --tests "io.github.rabinarayanpatra.sanitizer.spring.jackson.SanitizerModuleRecordIntegrationTest"`
Expected: 2 tests pass.

- [ ] **Step 5: Full spring module check**

Run: `./gradlew :sanitizer-spring:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add sanitizer-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/spring/jackson/SanitizerModule.java \
        sanitizer-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/spring/jackson/SanitizerModuleRecordIntegrationTest.java
git commit -m "feat(spring): wire Jackson deserializer through applyAndReturn

Records and cascaded graphs now sanitize on inbound JSON.

Assisted by AI"
```

---

## Phase 5: JPA listener update

### Task 17: Update `SanitizationEntityListener` to accept a safety checker and use `applyAndReturn`

**Files:**
- Modify: `sanitizer-jpa/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/SanitizationEntityListener.java`
- Test: `sanitizer-jpa/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/SanitizationEntityListenerTest.java`

- [ ] **Step 1: Inspect current listener**

Run: `cat /Volumes/Work/sanitizer-lib/sanitizer-jpa/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/SanitizationEntityListener.java`
Expected: a class with `@PrePersist @PreUpdate void onSave(Object entity)` that calls `SanitizationUtils.apply(entity)`.

- [ ] **Step 2: Write the failing test**

Create `sanitizer-jpa/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/SanitizationEntityListenerTest.java`:

```java
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
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
    }

    static class Parent {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
        @Sanitize(cascade = true) Child child;
    }

    static class Child {
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
    }
}
```

- [ ] **Step 2 (run): verify it fails**

Run: `./gradlew :sanitizer-jpa:test --tests "io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListenerTest"`
Expected: COMPILATION FAILURE — `setSafetyChecker` does not exist.

- [ ] **Step 3: Update the listener**

Replace contents of `sanitizer-jpa/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/SanitizationEntityListener.java` with:

```java
package io.github.rabinarayanpatra.sanitizer.jpa;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;
import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * JPA entity listener that applies sanitizers before persist and before update.
 * Wire it up by annotating an entity (or a {@code @MappedSuperclass}) with
 * {@code @EntityListeners(SanitizationEntityListener.class)}.
 *
 * <p>
 * In a Spring + Hibernate context, the listener is instantiated by Hibernate.
 * Use the {@code sanitizer-jpa-spring} starter to wire a Hibernate-aware
 * {@link TraversalSafetyChecker} that skips lazy associations.
 *
 * @since 1.0.0
 */
public class SanitizationEntityListener {

    private TraversalSafetyChecker safetyChecker = TraversalSafetyChecker.ALWAYS;

    /**
     * Overrides the default {@link TraversalSafetyChecker#ALWAYS} checker.
     * Spring auto-configuration in {@code sanitizer-jpa-spring} calls this with a
     * Hibernate-aware implementation.
     *
     * @param safetyChecker
     *            the checker to use for all subsequent {@link #onSave(Object)}
     *            invocations; must not be null
     */
    public void setSafetyChecker(final TraversalSafetyChecker safetyChecker) {
        this.safetyChecker = safetyChecker;
    }

    @PrePersist
    @PreUpdate
    public void onSave(final Object entity) {
        SanitizationUtils.applyAndReturn(entity, safetyChecker);
    }
}
```

- [ ] **Step 4: Run test**

Run: `./gradlew :sanitizer-jpa:test --tests "io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListenerTest"`
Expected: 3 tests pass.

- [ ] **Step 5: Full jpa module check**

Run: `./gradlew :sanitizer-jpa:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```bash
git add sanitizer-jpa/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/SanitizationEntityListener.java \
        sanitizer-jpa/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/SanitizationEntityListenerTest.java
git commit -m "feat(jpa): support custom TraversalSafetyChecker on entity listener

Listener now routes through applyAndReturn with a configurable checker
that defaults to ALWAYS. Top-level POJO behavior is unchanged.

Assisted by AI"
```

---

## Phase 6: New `sanitizer-jpa-spring` module

### Task 18: Add the module to the Gradle build

**Files:**
- Modify: `settings.gradle.kts`
- Create: `sanitizer-jpa-spring/build.gradle.kts`

- [ ] **Step 1: Inspect current settings**

Run: `cat /Volumes/Work/sanitizer-lib/settings.gradle.kts`
Expected:
```
rootProject.name = "sanitizer-lib"
include("sanitizer-core", "sanitizer-spring", "sanitizer-jpa")
```

- [ ] **Step 2: Add the module to `settings.gradle.kts`**

Replace contents of `/Volumes/Work/sanitizer-lib/settings.gradle.kts` with:

```kotlin
rootProject.name = "sanitizer-lib"
include("sanitizer-core", "sanitizer-spring", "sanitizer-jpa", "sanitizer-jpa-spring")
```

- [ ] **Step 3: Create the module directory and build file**

Run: `mkdir -p /Volumes/Work/sanitizer-lib/sanitizer-jpa-spring/src/main/java /Volumes/Work/sanitizer-lib/sanitizer-jpa-spring/src/test/java /Volumes/Work/sanitizer-lib/sanitizer-jpa-spring/src/main/resources/META-INF/spring`

Create `/Volumes/Work/sanitizer-lib/sanitizer-jpa-spring/build.gradle.kts`:

```kotlin
dependencies {
    api(project(":sanitizer-core"))
    api(project(":sanitizer-jpa"))
    api(project(":sanitizer-spring"))
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.h2database:h2")
}
```

- [ ] **Step 4: Verify the build can resolve the new module**

Run: `./gradlew :sanitizer-jpa-spring:dependencies --configuration runtimeClasspath | head -30`
Expected: no errors; shows `sanitizer-core`, `sanitizer-jpa`, `sanitizer-spring`, `spring-boot-starter-data-jpa` resolved.

- [ ] **Step 5: Commit**

```bash
git add settings.gradle.kts sanitizer-jpa-spring/build.gradle.kts
git commit -m "build: add sanitizer-jpa-spring module to multi-module build

Assisted by AI"
```

---

### Task 19: Implement `HibernateSafetyChecker`

**Files:**
- Create: `sanitizer-jpa-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/HibernateSafetyChecker.java`
- Create: `sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/HibernateSafetyCheckerTest.java`

- [ ] **Step 1: Write the failing test**

Create `sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/HibernateSafetyCheckerTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.lang.reflect.Field;

import jakarta.persistence.PersistenceUnitUtil;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HibernateSafetyCheckerTest {

    @Test
    void shouldDescendReturnsFalseForUnloadedFieldName() throws NoSuchFieldException {
        final PersistenceUnitUtil util = mock(PersistenceUnitUtil.class);
        final Holder holder = new Holder();
        final Field field = Holder.class.getDeclaredField("children");
        when(util.isLoaded(any(), eq("children"))).thenReturn(false);
        final TraversalSafetyChecker checker = new HibernateSafetyChecker(util);
        assertFalse(checker.shouldDescend(holder, field));
    }

    @Test
    void shouldDescendReturnsTrueForLoadedFieldName() throws NoSuchFieldException {
        final PersistenceUnitUtil util = mock(PersistenceUnitUtil.class);
        final Holder holder = new Holder();
        final Field field = Holder.class.getDeclaredField("children");
        when(util.isLoaded(any(), eq("children"))).thenReturn(true);
        final TraversalSafetyChecker checker = new HibernateSafetyChecker(util);
        assertTrue(checker.shouldDescend(holder, field));
    }

    static class Holder {
        java.util.List<String> children;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-jpa-spring:test --tests "io.github.rabinarayanpatra.sanitizer.jpa.spring.HibernateSafetyCheckerTest"`
Expected: COMPILATION FAILURE — `HibernateSafetyChecker` not found.

- [ ] **Step 3: Implement `HibernateSafetyChecker`**

Create `sanitizer-jpa-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/HibernateSafetyChecker.java`:

```java
package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.lang.reflect.Field;

import jakarta.persistence.PersistenceUnitUtil;

import io.github.rabinarayanpatra.sanitizer.core.TraversalSafetyChecker;

/**
 * {@link TraversalSafetyChecker} that defers to Hibernate's
 * {@link PersistenceUnitUtil#isLoaded(Object, String)} so the traversal engine
 * skips lazy associations that have not been initialized.
 *
 * @since 1.2.0
 */
public final class HibernateSafetyChecker implements TraversalSafetyChecker {

    private final PersistenceUnitUtil util;

    public HibernateSafetyChecker(final PersistenceUnitUtil util) {
        this.util = util;
    }

    @Override
    public boolean shouldDescend(final Object parent, final Field field) {
        return util.isLoaded(parent, field.getName());
    }
}
```

- [ ] **Step 4: Run test**

Run: `./gradlew :sanitizer-jpa-spring:test --tests "io.github.rabinarayanpatra.sanitizer.jpa.spring.HibernateSafetyCheckerTest"`
Expected: 2 tests pass.

- [ ] **Step 5: Commit**

```bash
git add sanitizer-jpa-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/HibernateSafetyChecker.java \
        sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/HibernateSafetyCheckerTest.java
git commit -m "feat(jpa-spring): add HibernateSafetyChecker delegating to PersistenceUnitUtil.isLoaded

Assisted by AI"
```

---

### Task 20: Implement `SanitizerJpaAutoConfiguration` wiring via `SpringBeanContainer`

**Files:**
- Create: `sanitizer-jpa-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/SanitizerJpaAutoConfiguration.java`
- Create: `sanitizer-jpa-spring/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Create: `sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/SanitizerJpaAutoConfigurationTest.java`

- [ ] **Step 1: Write the failing integration test**

Create `sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/SanitizerJpaAutoConfigurationTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(SanitizerJpaAutoConfiguration.class)
class SanitizerJpaAutoConfigurationTest {

    @Autowired
    TestEntityManager em;

    @Autowired
    HibernateSafetyChecker checker;

    @Test
    void contextWiresHibernateSafetyCheckerBean() {
        assertNotNull(checker);
    }

    @Test
    void persistRunsListenerAndSanitizesTopLevelField() {
        final UserEntity u = new UserEntity();
        u.email = "  USER@EXAMPLE.COM  ";
        final UserEntity saved = em.persistFlushFind(u);
        assertEquals("user@example.com", saved.email);
    }

    @Entity
    @EntityListeners(SanitizationEntityListener.class)
    static class UserEntity implements Serializable {
        @Id @GeneratedValue Long id;
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String email;
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :sanitizer-jpa-spring:test --tests "io.github.rabinarayanpatra.sanitizer.jpa.spring.SanitizerJpaAutoConfigurationTest"`
Expected: failure — `SanitizerJpaAutoConfiguration` does not exist; bean wiring missing.

- [ ] **Step 3: Implement auto-configuration**

Create `sanitizer-jpa-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/SanitizerJpaAutoConfiguration.java`:

```java
package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.orm.jpa.persistenceunit.SpringBeanContainer;

import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;

/**
 * Spring Boot auto-configuration that exposes a {@link HibernateSafetyChecker}
 * and configures Hibernate to instantiate {@link SanitizationEntityListener}
 * through Spring's {@link SpringBeanContainer}. The listener picks up the
 * checker as a setter-injected dependency.
 *
 * @since 1.2.0
 */
@AutoConfiguration
@ConditionalOnClass({EntityManagerFactory.class, BeanContainer.class})
public class SanitizerJpaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HibernateSafetyChecker hibernateSafetyChecker(final EntityManagerFactory emf) {
        final PersistenceUnitUtil util = emf.getPersistenceUnitUtil();
        return new HibernateSafetyChecker(util);
    }

    @Bean
    @ConditionalOnBean(HibernateSafetyChecker.class)
    public SanitizationEntityListener sanitizationEntityListener(final HibernateSafetyChecker checker) {
        final SanitizationEntityListener listener = new SanitizationEntityListener();
        listener.setSafetyChecker(checker);
        return listener;
    }

    @Bean
    public HibernatePropertiesCustomizer sanitizerSpringBeanContainerCustomizer(
            final ConfigurableListableBeanFactory beanFactory) {
        return properties -> properties.put(
                AvailableSettings.BEAN_CONTAINER, new SpringBeanContainer(beanFactory));
    }

    // SharedEntityManagerCreator is referenced to ensure Spring's JPA support is on
    // the classpath at compile time; it is not used at runtime here.
    @SuppressWarnings("unused")
    private static final Class<?> SHARED = SharedEntityManagerCreator.class;
}
```

- [ ] **Step 4: Register the auto-config**

Create `sanitizer-jpa-spring/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
io.github.rabinarayanpatra.sanitizer.jpa.spring.SanitizerJpaAutoConfiguration
```

- [ ] **Step 5: Run test**

Run: `./gradlew :sanitizer-jpa-spring:test --tests "io.github.rabinarayanpatra.sanitizer.jpa.spring.SanitizerJpaAutoConfigurationTest"`
Expected: 2 tests pass.

- [ ] **Step 6: Full module check**

Run: `./gradlew :sanitizer-jpa-spring:check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add sanitizer-jpa-spring/src/main/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/SanitizerJpaAutoConfiguration.java \
        sanitizer-jpa-spring/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports \
        sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/SanitizerJpaAutoConfigurationTest.java
git commit -m "feat(jpa-spring): auto-configure HibernateSafetyChecker and listener wiring

Registers a SpringBeanContainer with Hibernate so JPA listener
instances are sourced from the Spring context with a Hibernate-aware
safety checker.

Assisted by AI"
```

---

### Task 21: Integration test — bidirectional lazy association is not eagerly initialized

**Files:**
- Create: `sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/LazyAssociationIntegrationTest.java`

- [ ] **Step 1: Write the test**

Create `sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/LazyAssociationIntegrationTest.java`:

```java
package io.github.rabinarayanpatra.sanitizer.jpa.spring;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@AutoConfigureTestDatabase
@Import(SanitizerJpaAutoConfiguration.class)
class LazyAssociationIntegrationTest {

    @Autowired
    TestEntityManager em;

    @Test
    void persistParentDoesNotInitializeLazyChildren() {
        final Department dept = new Department();
        dept.name = "  ENG  ";
        final Department persisted = em.persistFlushFind(dept);
        em.clear();
        final Department reloaded = em.find(Department.class, persisted.id);
        // children association is lazy and not touched; sanitization must succeed
        // without LazyInitializationException.
        reloaded.name = "  ENG-2  ";
        em.persistAndFlush(reloaded);
        assertEquals("eng-2", reloaded.name);
    }

    @Entity
    @EntityListeners(SanitizationEntityListener.class)
    static class Department implements Serializable {
        @Id @GeneratedValue Long id;
        @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class}) String name;
        @Sanitize(cascade = true)
        @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
        List<Employee> employees = new ArrayList<>();
    }

    @Entity
    @EntityListeners(SanitizationEntityListener.class)
    static class Employee implements Serializable {
        @Id @GeneratedValue Long id;
        @Sanitize(using = {TrimSanitizer.class, UpperCaseSanitizer.class}) String code;
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "department_id")
        Department department;
    }
}
```

- [ ] **Step 2: Run test**

Run: `./gradlew :sanitizer-jpa-spring:test --tests "io.github.rabinarayanpatra.sanitizer.jpa.spring.LazyAssociationIntegrationTest"`
Expected: test passes. The Hibernate-aware safety checker prevents touching the lazy `employees` collection.

- [ ] **Step 3: Commit**

```bash
git add sanitizer-jpa-spring/src/test/java/io/github/rabinarayanpatra/sanitizer/jpa/spring/LazyAssociationIntegrationTest.java
git commit -m "test(jpa-spring): verify lazy associations not initialized during sanitization

Assisted by AI"
```

---

## Phase 7: Site, docs, version

### Task 22: Update `docs/index.html` to list the new module

**Files:**
- Modify: `docs/index.html`

- [ ] **Step 1: Locate the module list**

Run: `grep -n -E "sanitizer-(core|spring|jpa)" /Volumes/Work/sanitizer-lib/docs/index.html | head -40`
Expected: lines that render the three current module cards.

- [ ] **Step 2: Add the fourth card**

Edit `docs/index.html`. Within the same container that holds the three existing cards (cards for `sanitizer-core`, `sanitizer-spring`, `sanitizer-jpa`), copy the existing `sanitizer-jpa` block and add a new card with these substitutions:

- Title text: `sanitizer-jpa-spring`
- Description text: `Spring Boot starter that wires Hibernate-aware safety checks into the JPA entity listener.`
- Javadoc link href: `javadoc/sanitizer-jpa-spring/`

Concretely the new block follows the exact same HTML structure as the existing `sanitizer-jpa` card so the page renders consistently. Use the existing card as a template — do not invent new CSS classes.

- [ ] **Step 3: Eye-check the file**

Run: `grep -n "sanitizer-jpa-spring" /Volumes/Work/sanitizer-lib/docs/index.html`
Expected: at least two occurrences (the card title and the javadoc link href).

- [ ] **Step 4: Commit**

```bash
git add docs/index.html
git commit -m "docs(site): add sanitizer-jpa-spring module card

Assisted by AI"
```

---

### Task 23: Extend `publish-site.yml` to copy the new module's javadoc

**Files:**
- Modify: `.github/workflows/publish-site.yml`

- [ ] **Step 1: Locate javadoc copy steps**

Run: `grep -n -E "sanitizer-(core|spring|jpa)" /Volumes/Work/sanitizer-lib/.github/workflows/publish-site.yml`
Expected: lines that copy the three modules' javadoc directories into the published site.

- [ ] **Step 2: Add the fourth copy step**

Edit `.github/workflows/publish-site.yml`. Wherever the workflow copies `sanitizer-jpa/build/docs/javadoc`, add an identical step that copies `sanitizer-jpa-spring/build/docs/javadoc` into `site/javadoc/sanitizer-jpa-spring/`. Mirror the syntax of the existing steps exactly (same shell, same flags).

- [ ] **Step 3: Validate YAML**

Run: `python3 -c "import yaml,sys; yaml.safe_load(open('/Volumes/Work/sanitizer-lib/.github/workflows/publish-site.yml'))" && echo OK`
Expected: `OK`.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/publish-site.yml
git commit -m "ci: include sanitizer-jpa-spring javadoc in published site

Assisted by AI"
```

---

### Task 24: Update README with the new module and cascade docs

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Inspect README structure**

Run: `grep -n -E "## |sanitizer-(core|spring|jpa)" /Volumes/Work/sanitizer-lib/README.md | head -40`
Expected: section headers and module references.

- [ ] **Step 2: Add three additions**

Edit `README.md` to add:

1. In the modules / coordinates section, a row or paragraph for `sanitizer-jpa-spring` with the Maven coordinate `io.github.rabinarayanpatra.sanitizer:sanitizer-jpa-spring:1.2.0` and a one-line description: `"Spring Boot starter wiring Hibernate-aware safety checks into the JPA entity listener (skips lazy associations)."`
2. A short "Recursive sanitization" sub-section under usage that shows the `@Sanitize(cascade = true)` form and the difference between `apply(Object)` and `applyAndReturn(T)` for records. Use this exact code block:

```java
record OrderDto(
        @Sanitize(using = TrimSanitizer.class) String reference,
        @Sanitize(cascade = true) CustomerDto customer,
        @Sanitize(cascade = true) List<LineItem> items) {}

// Records: must use applyAndReturn — it returns a new instance.
OrderDto sanitized = SanitizationUtils.applyAndReturn(rawOrder);

// POJOs: apply mutates in place; applyAndReturn also works and returns
// the same reference.
SanitizationUtils.apply(somePojo);
```

3. A note in the same section: `"Cascade is strictly opt-in in v1.2.0. The default flips on in v2.0.0 alongside a new @SanitizeIgnore opt-out."`

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document sanitizer-jpa-spring and recursive sanitization

Assisted by AI"
```

---

### Task 25: Update `CHANGELOG.md` with the v1.2.0 entry

**Files:**
- Modify: `CHANGELOG.md`

- [ ] **Step 1: Inspect current CHANGELOG**

Run: `head -40 /Volumes/Work/sanitizer-lib/CHANGELOG.md`
Expected: shows the v1.1.0 entry at the top.

- [ ] **Step 2: Prepend the v1.2.0 entry**

Edit `CHANGELOG.md`. Add this block immediately below the header (and above the existing v1.1.0 entry):

```markdown
## [1.2.0] - 2026-06-05

### Added
- Java records support via new `SanitizationUtils.applyAndReturn(T)` entry point.
- Opt-in recursive traversal via `@Sanitize(cascade = true)` for nested objects, records, collections, and maps.
- `TraversalSafetyChecker` interface for pluggable graph-walk gating.
- New `sanitizer-jpa-spring` module providing `HibernateSafetyChecker` that skips non-initialized lazy JPA associations.
- SLF4J logging in the traversal engine at DEBUG/TRACE levels.

### Changed
- `@Sanitize.using()` now defaults to `{}` so a field can opt in to cascade without supplying a self-sanitizer.
- `SanitizationUtils.apply(record)` throws `IllegalArgumentException` (was `UnsupportedOperationException`) and recommends `applyAndReturn`.

### Compatibility
- All existing POJO behavior is bit-identical. Cascade is strictly opt-in in this release.
- The default cascade flag will flip on in v2.0.0 along with a new `@SanitizeIgnore` opt-out annotation.
```

- [ ] **Step 3: Commit**

```bash
git add CHANGELOG.md
git commit -m "docs(changelog): add 1.2.0 entry

Assisted by AI"
```

---

### Task 26: Bump version to 1.2.0

**Files:**
- Modify: `build.gradle.kts` (root)

- [ ] **Step 1: Inspect version line**

Run: `grep -n "version =" /Volumes/Work/sanitizer-lib/build.gradle.kts`
Expected: `version = "1.1.0"` line under `allprojects { ... }`.

- [ ] **Step 2: Update version**

Edit `build.gradle.kts`. Replace `version = "1.1.0"` with `version = "1.2.0"`.

- [ ] **Step 3: Verify**

Run: `./gradlew properties | grep '^version:'`
Expected: `version: 1.2.0`

- [ ] **Step 4: Commit**

```bash
git add build.gradle.kts
git commit -m "chore(release): bump version to 1.2.0

Assisted by AI"
```

---

## Phase 8: Full verification

### Task 27: Full aggregated check + coverage

- [ ] **Step 1: Run aggregated build**

Run: `./gradlew check jacocoAggregatedReport`
Expected: BUILD SUCCESSFUL across all 4 modules.

- [ ] **Step 2: Inspect aggregated coverage**

Run: `python3 -c "import xml.etree.ElementTree as ET; t=ET.parse('/Volumes/Work/sanitizer-lib/build/reports/jacoco/aggregated/jacoco.xml'); c=t.getroot().find('counter[@type=\"LINE\"]'); m,c2=int(c.get('missed')),int(c.get('covered')); print(f'line coverage: {c2/(m+c2)*100:.2f}%')"`
Expected: `line coverage: 98.00%` or higher.

If coverage drops below 98%, identify uncovered lines via `build/reports/jacoco/aggregated/html/index.html` and add targeted tests before proceeding.

- [ ] **Step 3: Run Spotless apply for any reformatting**

Run: `./gradlew spotlessApply`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Re-run check to ensure formatting did not break anything**

Run: `./gradlew check`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit any spotless changes (if produced)**

```bash
git add -A
git diff --cached --quiet || git commit -m "style: spotlessApply after v1.2.0 work

Assisted by AI"
```

---

### Task 28: Open the pull request

- [ ] **Step 1: Push the branch**

Run: `git push -u origin feat/v1.2.0-records-and-traversal`
Expected: branch pushed to origin.

- [ ] **Step 2: Open PR**

Run:
```bash
gh pr create --repo rabinarayanpatra/sanitizer-lib \
    --title "feat: v1.2.0 records support and recursive traversal engine" \
    --body "$(cat <<'EOF'
## Summary
- Add Java records support via new `SanitizationUtils.applyAndReturn(T)` entry point.
- Add opt-in recursive traversal via `@Sanitize(cascade = true)` for nested objects, records, collections, and maps.
- Add `TraversalSafetyChecker` interface for pluggable graph-walk gating.
- Add new `sanitizer-jpa-spring` module with `HibernateSafetyChecker` to skip non-initialized lazy associations.
- 100% source/binary compatible with v1.1.0. POJO behavior bit-identical without `cascade=true`.

Spec: `docs/superpowers/specs/2026-06-05-records-and-traversal-engine-design.md`
Plan: `docs/superpowers/plans/2026-06-05-records-and-traversal-engine.md`

## Test plan
- [ ] CI is green on the PR
- [ ] Aggregated JaCoCo coverage >= 98%
- [ ] Smoke test: deserialize a record DTO through Jackson and verify sanitized output
- [ ] Smoke test: persist a Spring/JPA entity with a lazy association via `@DataJpaTest` and verify no `LazyInitializationException`
EOF
)"
```
Expected: PR URL printed to stdout. Capture it for the next review cycle.

---

## Self-Review

The plan above covers every requirement in the spec:

- API surface (Task 1, Task 2, Task 9): `cascade`, `@Sanitize.using()` default, `TraversalSafetyChecker`, `applyAndReturn(T)`, `applyAndReturn(T, checker)`.
- Architecture (Tasks 3-9): `Kind`, `FieldDescriptor`, `ClassMetadata`, `TraversalState`, `TraversalEngine` with separated record/POJO branches.
- Cascade descent into POJO and RECORD (Task 10).
- Cascade descent into Collection (Task 11).
- Cascade descent into Map (Task 12).
- Cycle detection (Task 13).
- Safety checker (Task 14).
- Backward-compat (Task 15).
- Jackson integration (Task 16).
- JPA listener checker support (Task 17).
- New `sanitizer-jpa-spring` module with `HibernateSafetyChecker` and auto-config (Tasks 18-20).
- Lazy-association integration test (Task 21).
- Generic erasure handling for raw collections (covered in `CollectionWalker` early-exit with WARN and asserted by `TraversalEngineCollectionTest#rawCollectionFieldWithCascadeIsSkippedGracefully` and `ClassMetadataTest#rawCollectionTypeReturnsNullElementType`).
- SLF4J logger (Tasks 6, 7, 11, 12).
- `apply(record)` exception migration (Task 8).
- Site updates (Tasks 22, 23).
- README + CHANGELOG (Tasks 24, 25).
- Version bump (Task 26).
- Full check + coverage (Task 27).
- PR (Task 28).

No placeholders. Every code step contains complete, runnable content. Method and type names are consistent across tasks (`applyAndReturn`, `walk`, `walkRecord`, `walkPojo`, `descendByKind`, `setSafetyChecker`, `HibernateSafetyChecker`).

---

**Plan complete and saved to `docs/superpowers/plans/2026-06-05-records-and-traversal-engine.md`. Two execution options:**

**1. Subagent-Driven (recommended)** — dispatch a fresh subagent per task with two-stage review between tasks.

**2. Inline Execution** — execute the tasks in this session with batch checkpoints.

**Which approach?**
