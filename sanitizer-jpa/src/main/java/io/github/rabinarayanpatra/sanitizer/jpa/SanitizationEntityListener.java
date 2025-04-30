package io.github.rabinarayanpatra.sanitizer.jpa;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA entity listener that applies all {@link io.github.rabinarayanpatra.sanitizer.annotation.SanitizeField}
 * annotations to an entity before it is persisted or updated.
 * <p>
 * This listener automatically invokes {@link SanitizationUtils#apply(Object)} on any JPA entity to sanitize fields
 * marked with the appropriate annotations during {@code @PrePersist} and {@code @PreUpdate} lifecycle events.
 *
 * <pre>
 * {@code
 * @Entity
 * @EntityListeners(SanitizationEntityListener.class)
 * public class Customer {
 *   @SanitizeField(using = TrimSanitizer.class)
 *   private String name;
 * }
 * }
 * </pre>
 *
 * @since 1.0.0
 */
public class SanitizationEntityListener {

  /**
   * Lifecycle hook invoked before persisting or updating a JPA entity.
   *
   * @param entity the entity being saved
   */
  @PrePersist
  @PreUpdate
  public void onSave( final Object entity ) {
    if( entity != null ) {
      SanitizationUtils.apply( entity );
    }
  }
}

