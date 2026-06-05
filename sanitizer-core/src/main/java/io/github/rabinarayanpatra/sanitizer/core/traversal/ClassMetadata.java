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

	private static final Set<Class<?>> LEAF_TYPES = Set.of(String.class, Boolean.class, Byte.class, Character.class,
			Short.class, Integer.class, Long.class, Float.class, Double.class, UUID.class, LocalDate.class,
			LocalDateTime.class, Instant.class, BigDecimal.class, BigInteger.class);

	private final Class<?> type;
	private final boolean isRecord;
	private final @Nullable Constructor<?> canonicalCtor;
	private final RecordComponent @Nullable [] components;
	private final List<FieldDescriptor> fields;

	private ClassMetadata(final Class<?> type, final boolean isRecord, final @Nullable Constructor<?> canonicalCtor,
			final RecordComponent @Nullable [] components, final List<FieldDescriptor> fields) {
		this.type = type;
		this.isRecord = isRecord;
		this.canonicalCtor = canonicalCtor;
		this.components = components;
		this.fields = List.copyOf(fields);
	}

	static ClassMetadata of(final Class<?> cls) {
		return CACHE.computeIfAbsent(cls, ClassMetadata::build);
	}

	Class<?> type() {
		return type;
	}

	boolean isRecord() {
		return isRecord;
	}

	@Nullable
	Constructor<?> canonicalCtor() {
		return canonicalCtor;
	}

	RecordComponent @Nullable [] components() {
		return components;
	}

	List<FieldDescriptor> fields() {
		return fields;
	}

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
			throw new SanitizerInstantiationException("Cannot find canonical constructor for record " + cls.getName(),
					e);
		}
		final List<FieldDescriptor> descriptors = new ArrayList<>(components.length);
		for (final RecordComponent rc : components) {
			descriptors.add(describeRecordComponent(cls, rc));
		}
		return new ClassMetadata(cls, true, canonical, components, descriptors);
	}

	private static FieldDescriptor describeRecordComponent(final Class<?> owner, final RecordComponent rc) {
		final Sanitize[] anns = readRecordComponentAnnotations(owner, rc);
		final List<FieldSanitizer<Object>> chain = buildChain(anns);
		final boolean cascade = anyCascade(anns);
		final Class<?> declared = rc.getType();
		final Kind kind = resolveKind(declared);
		final Class<?> elementType = resolveElementType(rc.getGenericType(), kind);
		if (anns.length == 0) {
			// Unannotated record components produce pass-through descriptors so
			// canonical-constructor reconstruction can still copy the raw value.
			return FieldDescriptor.forRecordComponent(rc, chain, cascade, kind, elementType);
		}
		validateMeaningful(owner, rc.getName(), chain, cascade);
		validateCascadeKind(owner, rc.getName(), kind, cascade);
		return FieldDescriptor.forRecordComponent(rc, chain, cascade, kind, elementType);
	}

	private static Sanitize[] readRecordComponentAnnotations(final Class<?> owner, final RecordComponent rc) {
		final Sanitize[] direct = rc.getAnnotationsByType(Sanitize.class);
		if (direct.length > 0) {
			return direct;
		}
		// @Sanitize is @Target(FIELD), so annotations on a record component propagate
		// to the backing field rather than being visible via RecordComponent.
		try {
			final Field backing = owner.getDeclaredField(rc.getName());
			return backing.getAnnotationsByType(Sanitize.class);
		} catch (final NoSuchFieldException e) {
			return new Sanitize[0];
		}
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

	private static void validateMeaningful(final Class<?> owner, final String name,
			final List<FieldSanitizer<Object>> chain, final boolean cascade) {
		if (chain.isEmpty() && !cascade) {
			throw new IllegalStateException("@Sanitize on " + owner.getName() + "." + name
					+ " has no sanitizer and cascade=false. Either supply using={...} or set cascade=true.");
		}
	}

	private static void validateCascadeKind(final Class<?> owner, final String name, final Kind kind,
			final boolean cascade) {
		if (cascade && kind == Kind.LEAF) {
			throw new IllegalStateException("@Sanitize(cascade=true) on " + owner.getName() + "." + name
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
