package io.github.rabinarayanpatra.sanitizer.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Base class for sanitizers that accept configuration parameters.
 * <p>
 * Subclasses receive key-value parameters from the {@code @Sanitize} annotation
 * via {@link #configure(Map)} before any sanitization calls. Parameters are
 * specified using the {@code params} attribute of {@code @Sanitize}:
 *
 * <pre>
 * &#64;Sanitize(using = TruncateSanitizer.class, params = "maxLength=100")
 * private String description;
 * </pre>
 *
 * Multiple parameters can be separated by commas:
 *
 * <pre>
 * &#64;Sanitize(using = MaskSanitizer.class, params = "reveal=6,character=X")
 * private String accountNumber;
 * </pre>
 *
 * @param <T>
 *            the type of the field
 * @since 1.1.0
 */
public abstract class ConfigurableFieldSanitizer<T> implements FieldSanitizer<T> {

	private Map<String, String> params = Collections.emptyMap();

	/**
	 * Default constructor.
	 */
	protected ConfigurableFieldSanitizer() {
	}

	/**
	 * Called by the framework after instantiation to provide configuration
	 * parameters from the annotation.
	 *
	 * @param params
	 *            the configuration key-value pairs; never null, may be empty
	 */
	public void configure(final Map<String, String> params) {
		this.params = Collections.unmodifiableMap(new LinkedHashMap<>(params));
	}

	/**
	 * Returns the configuration parameters.
	 *
	 * @return unmodifiable map of parameter key-value pairs
	 */
	protected Map<String, String> getParams() {
		return params;
	}

	/**
	 * Returns a configuration parameter value, or a default if not present.
	 *
	 * @param key
	 *            the parameter key
	 * @param defaultValue
	 *            the default value if the key is absent
	 * @return the parameter value or the default
	 */
	protected String getParam(final String key, final String defaultValue) {
		return params.getOrDefault(key, defaultValue);
	}

	/**
	 * Returns a configuration parameter as an integer, or a default if not present
	 * or not parseable.
	 *
	 * @param key
	 *            the parameter key
	 * @param defaultValue
	 *            the default value if the key is absent or invalid
	 * @return the parameter value as int, or the default
	 */
	protected int getIntParam(final String key, final int defaultValue) {
		final @Nullable String value = params.get(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (final NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Parses a comma-separated {@code key=value} parameter string into a map.
	 *
	 * @param paramString
	 *            the raw parameter string from the annotation
	 * @return a mutable map of parsed key-value pairs
	 */
	public static Map<String, String> parseParams(final String paramString) {
		final Map<String, String> result = new LinkedHashMap<>();
		if (paramString == null || paramString.isBlank()) {
			return result;
		}
		for (final String pair : paramString.split(",", -1)) {
			final String trimmed = pair.trim();
			final int eq = trimmed.indexOf('=');
			if (eq > 0) {
				result.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
			}
		}
		return result;
	}
}
