# Records support and recursive traversal engine

**Status:** Draft
**Date:** 2026-06-05
**Target release:** v1.2.0
**Owner:** rabinarayanpatra

## Goal

Add first-class Java records support to `sanitizer-lib` and unify it with a recursive traversal engine that walks nested POJOs, records, collections, and maps. Ship as a strictly backward-compatible minor release.

## Non-goals

- Default-on cascade behavior (deferred to v2.0.0; opt-in only in v1.2.0)
- Spring Boot 4 / Jackson 3 BOM upgrade (separate ticket)
- Caffeine weak-key cache swap (separate ticket)
- jqwik property-based tests (separate ticket)
- `@SanitizeIgnore` opt-out annotation (introduced in v2.0.0 alongside default-on cascade)
- Bean Validation adapter (separate ticket)

## Background

Current `SanitizationUtils.apply(Object)` throws `UnsupportedOperationException` on records because record components are final and cannot be set via reflection. The utility also walks only the top-level declared fields of a POJO: nested objects, collections of records, and map values are not sanitized even when their types carry `@Sanitize` annotations.

For a library positioned as "enterprise-grade input sanitization", surface-only sanitization is a leaky boundary. Real DTO graphs (`OrderDto -> CustomerDto -> AddressDto`, `List<LineItem>`) require recursion.

## Scope decisions

| Decision | Choice | Reason |
|---|---|---|
| Recursion scope | Full graph: records + POJOs + collections + maps | Production-grade lib must not stop at surface |
| Default cascade | Opt-in per field via `cascade=true` (v1.2.0); flips on in v2.0.0 | SemVer minor must not change behavior for existing inputs |
| Records reconstruction | Canonical constructor invocation, immutable-style | Only legal mechanism for record component mutation |
| API shape | Add `applyAndReturn(T)`; keep `apply(Object)` void with throw on record root | Existing POJO callers untouched; record root would silently discard if void-returned |
| Collection element rebuild | Rebuild when element is record or collection is unmodifiable; otherwise mutate in place | Records and `List.of()` are immutable; mutable collections benefit from in-place |
| Map handling | Walk values only, keys untouched | Keys typically serve as IDs and are not annotated |
| Cycle detection | `IdentityHashMap` visited-set per traversal | Prevents `StackOverflowError` on bidirectional JPA graphs |
| JPA lazy safety | Skip non-initialized associations via `PersistenceUnitUtil.isLoaded` | Prevents N+1 query storms and `LazyInitializationException` at flush |
| JPA wiring path | New `sanitizer-jpa-spring` module for Hibernate-aware wiring | Keeps `sanitizer-jpa` framework-free; clean layering |
| Architecture | `TraversalEngine` + `ClassMetadata` + `FieldDescriptor` in core | Separates per-class plan from runtime walker; testable units |

## Public API surface (v1.2.0)

### Annotations (`io.github.rabinarayanpatra.sanitizer.annotation`)

`@Sanitize` gains an optional `cascade` attribute. The existing `using` attribute becomes optional (default `{}`) so a field can opt in to cascade without supplying a self-sanitizer.

```java
@Retention(RUNTIME)
@Target(FIELD)
@Repeatable(Sanitizes.class)
public @interface Sanitize {
    Class<? extends FieldSanitizer<?>>[] using() default {};
    String params() default "";
    boolean cascade() default false;
}
```

Annotation usage matrix:

| Annotation form | Effect |
|---|---|
| `@Sanitize(using = Trim.class)` | Sanitize this field's value. Top-level only. Existing behavior. |
| `@Sanitize(cascade = true)` | Descend into this field's object graph. No self-sanitizer applied. |
| `@Sanitize(using = Trim.class, cascade = true)` | Sanitize value, then descend. |
| Field without `@Sanitize` | Untouched. Existing behavior. |

### `SanitizationUtils` (`io.github.rabinarayanpatra.sanitizer.core`)

```java
public final class SanitizationUtils {
    // EXISTING (semantics preserved for POJO roots; throws IllegalArgumentException for record roots)
    public static void apply(@Nullable Object bean);

    // NEW universal entry
    public static <T> @Nullable T applyAndReturn(@Nullable T bean);
    public static <T> @Nullable T applyAndReturn(@Nullable T bean, TraversalSafetyChecker checker);
}
```

### `TraversalSafetyChecker` (`io.github.rabinarayanpatra.sanitizer.core`)

```java
public interface TraversalSafetyChecker {
    boolean shouldDescend(Object parent, java.lang.reflect.Field field);
    TraversalSafetyChecker ALWAYS = (parent, field) -> true;
}
```

JPA module ships a Hibernate-aware implementation; non-Spring/non-JPA users get `ALWAYS` by default.

### Behavior contract for `applyAndReturn`

| Input | Behavior | Returned reference |
|---|---|---|
| `null` | No-op | `null` |
| POJO instance | Mutate fields in place per `FieldDescriptor` plan | Same instance |
| Record instance | Build new instance via canonical constructor with sanitized components | New instance |

### Behavior contract for legacy `apply(Object)`

- POJO root: identical to existing behavior; void return.
- Record root: throws `IllegalArgumentException` with message:
  > `apply(Object) discards return value but record requires reassignment. Use applyAndReturn(T) for record type X.`
- Previously threw `UnsupportedOperationException`; exception type change is intentional and documented in CHANGELOG.

## Architecture

### Package layout (sanitizer-core)

```
io.github.rabinarayanpatra.sanitizer.core/
    SanitizationUtils                    // public facade
    FieldSanitizer                       // existing
    ConfigurableFieldSanitizer           // existing
    SanitizerInstantiationException      // existing
    TraversalSafetyChecker               // new, public

io.github.rabinarayanpatra.sanitizer.core.traversal/   // package-private internals
    TraversalEngine
    ClassMetadata
    FieldDescriptor
```

### `ClassMetadata`

Built once per class, cached in `ConcurrentHashMap<Class<?>, ClassMetadata>`.

```java
final class ClassMetadata {
    final Class<?> type;
    final boolean isRecord;
    final Constructor<?> canonicalCtor;        // null when !isRecord
    final RecordComponent[] components;        // null when !isRecord
    final List<FieldDescriptor> fields;        // walk plan, hierarchy-flattened
}
```

For POJOs the walker iterates `fields` in declaration order, walking `getSuperclass()` chain to support `@MappedSuperclass` (existing behavior).

For records the walker iterates `components` in canonical order, building an `Object[]` of arguments for `canonicalCtor.newInstance(args)`.

### `FieldDescriptor`

```java
final class FieldDescriptor {
    final Field field;                              // null for record components
    final RecordComponent recordComponent;          // null for POJO fields
    final List<FieldSanitizer<Object>> chain;       // ordered self-sanitizers (may be empty)
    final boolean cascade;                          // descend flag
    final Kind kind;                                // LEAF | POJO | RECORD | COLLECTION | MAP
    final Class<?> elementType;                     // resolved for COLLECTION/MAP value; null otherwise
}

enum Kind { LEAF, POJO, RECORD, COLLECTION, MAP }
```

`Kind` resolution at metadata build:

| Type predicate | Kind |
|---|---|
| `String`, primitives, boxed primitives, `UUID`, `LocalDate`, `LocalDateTime`, `Instant`, `BigDecimal`, `BigInteger`, `Enum` | `LEAF` |
| `java.lang.Record` subclass | `RECORD` |
| `Collection<?>` (resolved element type from `ParameterizedType`) | `COLLECTION` |
| `Map<?,?>` (resolved value type) | `MAP` |
| Anything else | `POJO` |

If `cascade=true` on a `LEAF` field, metadata build throws `IllegalStateException` with field path and resolved type.

### `TraversalEngine.walk`

Single entry point invoked by the facade. Signature:

```java
@Nullable Object walk(@Nullable Object node,
                      TraversalState state,
                      TraversalSafetyChecker checker);
```

`TraversalState` bundles two maps allocated once per top-level `applyAndReturn` invocation:

```java
final class TraversalState {
    final IdentityHashMap<Object, Boolean> visited;          // POJOs, collections, maps
    final IdentityHashMap<Object, Object> reconstructedRecords; // original record -> new instance
}
```

Algorithm:

```
if node == null
    -> return null

meta = CACHE.computeIfAbsent(node.getClass(), ClassMetadata::of)

if meta.isRecord:
    if state.reconstructedRecords.containsKey(node):
        return state.reconstructedRecords.get(node)    // shared record ref: return same new instance
    Object built = walkRecord(node, meta, state, checker)
    state.reconstructedRecords.put(node, built)
    return built

if state.visited.put(node, TRUE) != null:
    return node                                         // cycle: revisit short-circuits

walkPojo(node, meta, state, checker)                    // mutates in place
return node
```

Record sharing across graph positions is handled correctly: the second visit returns the same reconstructed instance, so two parents referencing the same original record both see the same sanitized output.

`walkPojo` per `FieldDescriptor`:

1. Read raw value.
2. Apply `chain` in order; write final result to field.
3. If `cascade && checker.shouldDescend(node, field)`:
   - `POJO`: `walk(child, state, checker)` (in-place; ignore return)
   - `RECORD`: `newRef = walk(child, state, checker)`; write `newRef` back to field
   - `COLLECTION`: `walkCollection(coll, elementType, descriptor.chain, state, checker)` (may return new collection ref to write back)
   - `MAP`: `walkMapValues(map, valueType, descriptor.chain, state, checker)` (may return new map ref)
   - `LEAF`: unreachable (rejected at metadata build)

`walkRecord`:

```
Object[] args = new Object[meta.components.length]
for i in 0..n-1:
    Object raw = component[i].accessor().invoke(node)
    Object sanitized = applyChain(raw, descriptor[i].chain)
    if descriptor[i].cascade and sanitized != null:
        sanitized = recurseByKind(sanitized, descriptor[i], state, checker)
    args[i] = sanitized
return meta.canonicalCtor.newInstance(args)
```

`walkCollection`:

Determine element handling from element type's `ClassMetadata`. Detect unmodifiability via a class-name allowlist (cached): `java.util.ImmutableCollections$*` (covers `List.of`, `Set.of`, `Map.of`) and `java.util.Collections$Unmodifiable*` (covers `Collections.unmodifiable*`). Probing via mutation is rejected: too many false negatives (e.g., `Arrays.asList` allows `set` but not `add`).

Rules:

- Element type is `RECORD` OR collection class is unmodifiable: build new `ArrayList` for `List`/`Queue`/`Deque`/other `Collection`, or `LinkedHashSet` for `Set`. Populate with sanitized elements. Return new collection (caller writes back to field).
- Element type is `POJO`, collection is mutable: walk each element in place via `walk(elem, state, checker)`. Collection ref unchanged. Return original.
- Element type is `LEAF` (e.g., `List<String>`) with sanitizer chain on the field: apply chain element-wise. `List`: `list.set(i, sanitized)` if identity changed. `Set`: rebuild as `LinkedHashSet` (hash may change). Return original ref if `List`, new ref if `Set`.

`walkMapValues`: same logic on `map.values()`; keys never inspected.

### Cycle detection

`TraversalState` (one `visited` map plus one `reconstructedRecords` map) is allocated once per `applyAndReturn` invocation. Cost: two maps + entries. Revisits to the same POJO/collection/map return the existing reference without re-sanitizing. Revisits to the same original record return the previously reconstructed instance.

Records cannot form record-only cycles: every record component is final, so a back-edge requires a mutable interior (collection, map, or POJO), which is cycle-checked at its own boundary.

### Generic type erasure handling

For `Collection` and `Map` fields, the element/value type is resolved from `Field.getGenericType()` via `ParameterizedType`. If the resolved type is a wildcard, type variable, or raw type with no concrete element class, the engine cannot determine element handling. In that case:

- If the field has `cascade=true`, log WARN once per field and skip descent.
- If the field carries a leaf sanitizer chain only, apply the chain element-wise assuming `Object` and let the existing `ClassCastException` diagnostic fire on type mismatch (preserves current behavior).

### Class metadata cache

`ConcurrentHashMap<Class<?>, ClassMetadata> CACHE`. Same shape as today's `SanitizationUtils.CACHE`. Class-loader leak risk acknowledged; Caffeine weak-key swap deferred to a separate ticket.

## Module changes

### sanitizer-core

- New: `TraversalEngine`, `ClassMetadata`, `FieldDescriptor` in `core.traversal` (package-private)
- New: `TraversalSafetyChecker` interface in `core` (public)
- Modified: `SanitizationUtils` becomes thin facade delegating to `TraversalEngine`
- Modified: `Sanitize` annotation adds `cascade()` attribute and makes `using()` default to `{}`

### sanitizer-spring

- Modified: `SanitizerModule.SanitizingDeserializer.deserialize` replaces `apply(bean); return bean` with `return SanitizationUtils.applyAndReturn(bean)`
- No changes to `SanitizerAutoConfiguration`, `SanitizerRegistry`

### sanitizer-jpa

- Modified: `SanitizationEntityListener` adds optional setter `setSafetyChecker(TraversalSafetyChecker)` (defaults to `ALWAYS`)
- Modified: `onSave` calls `SanitizationUtils.applyAndReturn(entity, checker)`
- No new framework dependency

### sanitizer-jpa-spring (NEW MODULE)

- Depends on `sanitizer-jpa`, `sanitizer-spring`, `spring-boot-starter-data-jpa`
- New: `HibernateSafetyChecker implements TraversalSafetyChecker` — delegates `shouldDescend` to `PersistenceUnitUtil.isLoaded(parent, fieldName)`
- New: `SanitizerJpaAutoConfiguration` — registers `HibernateSafetyChecker` bean from `EntityManagerFactory.getPersistenceUnitUtil()`. JPA listeners are instantiated by the JPA provider, not Spring, so wiring uses Spring's `SpringBeanContainer` (registered via `HibernatePropertiesCustomizer` setting `hibernate.resource.beans.container`). The listener's no-arg constructor reads the safety checker through this container at bean-creation time; if no Spring context is present the listener falls back to `TraversalSafetyChecker.ALWAYS`.

### Build configuration

- `settings.gradle.kts`: add `include("sanitizer-jpa-spring")`
- Root `build.gradle.kts`: existing publishing config covers the new module automatically (subprojects loop)
- `docs/index.html`: add fourth module card linking to its Javadoc
- `.github/workflows/publish-site.yml`: extend site assembly to copy `sanitizer-jpa-spring/build/docs/javadoc`

## Error handling

| Failure | Exception | Notes |
|---|---|---|
| `apply(record)` | `IllegalArgumentException` | Message names the record class and points to `applyAndReturn` |
| `@Sanitize(cascade=true)` on LEAF field | `IllegalStateException` at metadata build | Message includes class.field path and resolved type |
| Record canonical ctor throws | `InvocationTargetException` cause unwrapped and rethrown | Preserves user's compact-constructor validation message |
| Sanitizer instantiation fails | `SanitizerInstantiationException` | Existing; unchanged |
| Field type mismatch with sanitizer generic | `IllegalStateException` | Existing; unchanged |
| Unmodifiable collection encountered during cascade | No exception; engine rebuilds | Silent recovery |
| Cycle detected | No exception; visited check short-circuits | Silent recovery |
| JPA lazy field not loaded | No exception; `shouldDescend` returns false | Silent skip |

### Logging

`TraversalEngine` adds an SLF4J `Logger`. Levels:

- `DEBUG`: `walk class={} sanitizers={} cascade={}`
- `TRACE`: per-field action (`sanitize field={} kind={} cascade={}`)

No `INFO`/`WARN` at hot path.

## Compatibility statement

### Source compatibility

100%. No public types removed, renamed, re-signatured, or moved. New types (`applyAndReturn`, `TraversalSafetyChecker`, `cascade` attribute) are purely additive.

### Binary compatibility

100%. Old `apply(Object)` retains its signature and throws-pattern (only the thrown exception type changes: `UnsupportedOperationException` -> `IllegalArgumentException` for record root inputs, which were already a failure case).

### Behavioral compatibility

100% for any code that does not opt in to cascade. The walker's default behavior on a POJO field without `cascade=true` is identical to the current implementation.

Records that previously caused `UnsupportedOperationException` now have two valid paths:

- `applyAndReturn(record)` returns a sanitized new instance (new functionality)
- `apply(record)` throws `IllegalArgumentException` (was `UnsupportedOperationException`)

Users who catch the previous exception type by name and depend on the message string must migrate. Documented in CHANGELOG migration note.

### CHANGELOG entry sketch

> ## [1.2.0]
> ### Added
> - Java records support via new `SanitizationUtils.applyAndReturn(T)` entry point
> - Opt-in recursive traversal via `@Sanitize(cascade = true)` for nested objects, records, collections, and maps
> - `TraversalSafetyChecker` interface for pluggable graph-walk gating
> - New `sanitizer-jpa-spring` module providing `HibernateSafetyChecker` that skips non-initialized lazy JPA associations
> - SLF4J logging in the traversal engine (DEBUG/TRACE)
>
> ### Changed
> - `@Sanitize.using()` now defaults to `{}` so a field can opt in to cascade without supplying a self-sanitizer
> - `SanitizationUtils.apply(record)` throws `IllegalArgumentException` (was `UnsupportedOperationException`) and recommends `applyAndReturn`
>
> ### Compatibility
> - All existing POJO behavior is bit-identical. Cascade is strictly opt-in for this release.
> - The default cascade flag will flip on in v2.0.0 along with a new `@SanitizeIgnore` opt-out annotation.

## Testing strategy

### sanitizer-core

New tests under `src/test/java/.../core/traversal/`:

- `TraversalEngineRecordTest` — single record, nested records, null components, ctor validation propagation
- `TraversalEngineCascadeTest` — `cascade=true` on POJO, on record component, mixed POJO/record graphs, default-off backward compat assertion
- `TraversalEngineCollectionTest` — `List<String>` leaf sanitizer, `List<Record>` rebuild, unmodifiable list rebuild, `Set` order preservation, mutable POJO list in-place mutation
- `TraversalEngineMapTest` — values walked, keys untouched, value-record rebuild
- `TraversalEngineCycleTest` — bidirectional POJO graph traversal terminates, label sanitized once per node
- `TraversalEngineSafetyCheckerTest` — custom checker returning false skips descent
- `ClassMetadataTest` — metadata built correctly for record, POJO with superclass, generic Collection element type resolution
- `FieldDescriptorKindResolutionTest` — every Kind branch covered; `cascade=true` on LEAF throws

Fixtures (test-scope only):

```java
record Address(@Sanitize(using = TrimSanitizer.class) String city,
               @Sanitize(using = UpperCaseSanitizer.class) String country) {}

record Customer(@Sanitize(using = TrimSanitizer.class) String name,
                @Sanitize(cascade = true) Address address) {}

record Order(@Sanitize(cascade = true) Customer customer,
             @Sanitize(cascade = true) List<LineItem> items) {}

record LineItem(@Sanitize(using = TrimSanitizer.class) String sku,
                @Sanitize(using = UpperCaseSanitizer.class) String code) {}

class MutableParent {
    @Sanitize(using = TrimSanitizer.class) String name;
    @Sanitize(cascade = true) Address embeddedRecord;
    @Sanitize(cascade = true) List<Customer> customers;
    @Sanitize(cascade = true) Map<String, LineItem> items;
}

class CyclicNode {
    @Sanitize(using = TrimSanitizer.class) String label;
    @Sanitize(cascade = true) CyclicNode next;
}
```

### Backward-compat regression suite

- All 18 existing builtin sanitizer tests pass unchanged
- Existing `SanitizationUtilsTest` and `ConfigurableFieldSanitizerTest` scenarios pass unchanged
- New explicit test: a POJO with a nested annotated field but without `cascade=true` leaves the nested field untouched after `apply(root)`

### sanitizer-jpa

- `HibernateSafetyCheckerTest` — Mockito-backed `PersistenceUnitUtil`; assert `shouldDescend` returns false for unloaded field name and true for loaded
- Integration test with H2 + Hibernate: bidirectional `@OneToMany`/`@ManyToOne` entity, persist parent with un-fetched children, assert no `LazyInitializationException` and child not sanitized

### sanitizer-jpa-spring

- `SanitizerJpaAutoConfigurationTest` — verify `HibernateSafetyChecker` bean exposed and wired into `SanitizationEntityListener`
- End-to-end `@DataJpaTest` slice persisting a sanitized entity via the listener

### sanitizer-spring

- Existing `SanitizerModuleIntegrationTest` extended with a record DTO case
- New test: nested record DTO with `cascade=true` round-trips through Jackson

### Coverage target

Aggregated JaCoCo coverage stays at or above the current floor (98%).

## Deferred items

| Item | Target |
|---|---|
| Default cascade flips on, `@SanitizeIgnore` introduced | v2.0.0 |
| Caffeine weak-key `ClassMetadata` cache | Separate ticket |
| jqwik property-based tests for sanitizer purity | Separate ticket |
| Bean Validation `ConstraintValidator` adapter | Separate ticket |
| Spring Boot 4 / Jackson 3 BOM upgrade | Separate ticket |
| GraalVM `RuntimeHints` registration for AOT/native | Separate ticket |

## Open questions

None. All design decisions resolved during brainstorming.
