package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;

/**
 * Utility class that applies {@link FieldSanitizer} logic to bean fields
 * annotated with {@link Sanitize}.
 *
 * @since 1.0.0
 */
public final class SanitizationUtils {

	private static final Map<Class<?>, List<Holder>> CACHE = new ConcurrentHashMap<>();

	private SanitizationUtils() {
	}

	/**
	 * Applies all configured sanitizers to the fields of the given bean that are
	 * annotated with {@link Sanitize}.
	 *
	 * @param bean
	 *            the object whose fields should be sanitized
	 */
	public static void apply(final Object bean) {
		if (bean == null) {
			return;
		}

		final Class<?> cls = bean.getClass();
		final List<Holder> holders = CACHE.computeIfAbsent(cls, SanitizationUtils::inspect);

		for (final Holder h : holders) {
			try {
				final Field f = h.field;
				final Object raw = f.get(bean);
				if (raw == null) {
					final Object clean = h.sanitizer.sanitize(null);
					if (clean != null) {
						f.set(bean, clean);
					}
					continue;
				}
				final Object clean = h.sanitizer.sanitize(raw);
				f.set(bean, clean);
			} catch (final IllegalAccessException e) {
				throw new IllegalStateException(
						"Cannot access field '" + h.field.getName() + "' on " + bean.getClass().getName()
								+ ". This may occur with Java records, JPMS strongly-encapsulated fields, "
								+ "or final fields. Ensure the field is mutable and accessible.",
						e);
			} catch (final ClassCastException e) {
				throw new IllegalStateException("Type mismatch: sanitizer " + h.sanitizer.getClass().getName()
						+ " is not compatible with field '" + h.field.getName() + "' of type "
						+ h.field.getType().getName() + " on " + bean.getClass().getName()
						+ ". Ensure the sanitizer's generic type matches the field type.", e);
			}
		}
	}

	/**
	 * Inspects the class hierarchy to find fields annotated with {@link Sanitize}
	 * and builds sanitizer handlers. Walks superclasses to support
	 * {@code @MappedSuperclass} and other inheritance patterns.
	 */
	private static List<Holder> inspect(final Class<?> cls) {
		final List<Holder> list = new ArrayList<>();

		Class<?> current = cls;
		while (current != null && current != Object.class) {
			for (final Field field : current.getDeclaredFields()) {
				final Sanitize[] annotations = field.getAnnotationsByType(Sanitize.class);
				if (annotations.length == 0) {
					continue;
				}

				field.setAccessible(true);
				for (final Sanitize ann : annotations) {
					for (final Class<? extends FieldSanitizer<?>> sanitizerClass : ann.using()) {
						try {
							@SuppressWarnings("unchecked")
							final FieldSanitizer<Object> sanitizer = (FieldSanitizer<Object>) sanitizerClass
									.getDeclaredConstructor().newInstance();
							list.add(new Holder(field, sanitizer));
						} catch (final ReflectiveOperationException e) {
							throw new SanitizerInstantiationException(
									"Cannot instantiate sanitizer " + sanitizerClass.getName(), e);
						}
					}
				}
			}
			current = current.getSuperclass();
		}

		return list;
	}

	/**
	 * Binds a {@link Field} with its corresponding {@link FieldSanitizer}.
	 */
	private record Holder(Field field, FieldSanitizer<Object> sanitizer) {
	}
}
