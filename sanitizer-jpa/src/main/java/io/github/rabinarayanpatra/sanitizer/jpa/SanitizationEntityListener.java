package io.github.rabinarayanpatra.sanitizer.jpa;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA entity listener that applies all @SanitizeField rules on entities before they are inserted or updated.
 */
public class SanitizationEntityListener {

  @PrePersist
  @PreUpdate
  public void onSave( Object entity ) {
    SanitizationUtils.apply( entity );
  }
}
