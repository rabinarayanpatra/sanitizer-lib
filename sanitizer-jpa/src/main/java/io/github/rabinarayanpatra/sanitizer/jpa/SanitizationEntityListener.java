package io.github.rabinarayanpatra.sanitizer.jpa;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA entity listener that automatically applies sanitization to any entity
 * fields annotated with {@link Sanitize @Sanitize} before they are persisted or
 * updated.
 * <p>
 * Internally this listener invokes {@link SanitizationUtils#apply(Object)} to
 * run through all configured
 * {@link io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer}s.
 *
 * <pre>
 * {
 * 	&#64;code
 * 	&#64;Entity
 * 	&#64;EntityListeners(SanitizationEntityListener.class)
 * 	public class Customer {
 *
 * 		&#64;Sanitize(using = TrimSanitizer.class)
 * 		&#64;Sanitize(using = CollapseWhitespaceSanitizer.class)
 * 		private String name;
 *
 * 		// getters/setters...
 * 	}
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public class SanitizationEntityListener {

	/**
	 * Default constructor.
	 */
	public SanitizationEntityListener() {
	}

	/**
	 * Lifecycle callback invoked by JPA before an entity is inserted or updated.
	 * <p>
	 * Calls {@link SanitizationUtils#apply(Object)} on the given entity instance,
	 * sanitizing any fields marked with {@link Sanitize @Sanitize}.
	 *
	 * @param entity
	 *               the entity instance about to be persisted or updated; never
	 *               {@code null}
	 */
	@PrePersist
	@PreUpdate
	public void onSave(final Object entity) {
		SanitizationUtils.apply(entity);
	}
}
