package io.github.rabinarayanpatra.sanitizer.spring.registry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

/**
 * Spring-based registry for resolving {@link FieldSanitizer} instances by class
 * type.
 * <p>
 * This component is autoconfigured by Spring and automatically populated with
 * all available {@link FieldSanitizer} beans, allowing reusable lookup and
 * injection in places like entity listeners or deserializers.
 *
 * @since 1.0.0
 */
public class SanitizerRegistry {

	/**
	 * Holds sanitizer instances keyed by their implementation class.
	 */
	private final Map<Class<? extends FieldSanitizer<?>>, FieldSanitizer<?>> registry = new ConcurrentHashMap<>();

	/**
	 * Constructs the registry by collecting all {@link FieldSanitizer} beans
	 * available in the Spring context.
	 *
	 * @param sanitizers
	 *            the list of all discovered sanitizers
	 */
	@Autowired
	public SanitizerRegistry(final List<FieldSanitizer<?>> sanitizers) {
		for (final FieldSanitizer<?> sanitizer : sanitizers) {
			@SuppressWarnings("unchecked")
			final Class<? extends FieldSanitizer<?>> cls = (Class<? extends FieldSanitizer<?>>) sanitizer.getClass();
			registry.put(cls, sanitizer);
		}
	}

	/**
	 * Retrieves a registered sanitizer instance by its class.
	 *
	 * @param <T>
	 *            the type the sanitizer handles
	 * @param cls
	 *            the sanitizer implementation class
	 * @return the matching sanitizer instance
	 * @throws IllegalArgumentException
	 *             if no matching sanitizer is found
	 */
	@SuppressWarnings("unchecked")
	public <T> FieldSanitizer<T> get(final Class<? extends FieldSanitizer<T>> cls) {
		final FieldSanitizer<?> sanitizer = registry.get(cls);

		if (sanitizer == null) {
			throw new IllegalArgumentException("No sanitizer registered for " + cls.getName());
		}

		return (FieldSanitizer<T>) sanitizer;
	}
}
