package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.rabinarayanpatra.sanitizer.annotation.SanitizeField;

/**
 * Utility class that applies {@link FieldSanitizer} logic to bean fields annotated with {@link SanitizeField}.
 * <p>
 * This class uses reflection to inspect fields and apply one or more sanitizers declared via annotations. Sanitizer
 * instances are cached per class for performance.
 *
 * @since 1.0.0
 */
public final class SanitizationUtils {

  private static final Map<Class<?>, List<Holder>> CACHE = new ConcurrentHashMap<>();

  private SanitizationUtils() {
    // Utility class; prevent instantiation
  }

  /**
   * Applies all configured sanitizers to the fields of the given bean that are annotated with {@link SanitizeField}.
   * <p>
   * This method is a no-op if the bean is {@code null}, or if no fields are annotated.
   *
   * @param bean the object whose fields should be sanitized
   */
  public static void apply( final Object bean ) {
    if( bean == null ) {
      return;
    }

    final Class<?> cls = bean.getClass();
    final List<Holder> holders = CACHE.computeIfAbsent( cls, SanitizationUtils::inspect );

    for( final Holder h : holders ) {
      try {
        final Field field = h.field;
        final Object raw = field.get( bean );
        final Object clean = h.sanitizer.sanitize( raw );
        field.set( bean, clean );
      } catch( final IllegalAccessException ignored ) {
        // Field should be accessible due to setAccessible(true); safe to ignore
      }
    }
  }

  /**
   * Inspects the class to find fields annotated with {@link SanitizeField} and builds sanitizer handlers.
   *
   * @param cls the class to inspect
   * @return a list of sanitizer-field holders
   */
  private static List<Holder> inspect( final Class<?> cls ) {
    final List<Holder> list = new ArrayList<>();

    for( final Field field : cls.getDeclaredFields() ) {
      final SanitizeField[] annotations = field.getAnnotationsByType( SanitizeField.class );
      if( annotations.length == 0 ) {
        continue;
      }

      field.setAccessible( true );

      for( final SanitizeField ann : annotations ) {
        try {
          @SuppressWarnings( "unchecked" ) final FieldSanitizer<Object> sanitizer = (FieldSanitizer<Object>) ann.using()
              .getDeclaredConstructor().newInstance();

          list.add( new Holder( field, sanitizer ) );
        } catch( final ReflectiveOperationException e ) {
          throw new SanitizerInstantiationException( "Cannot instantiate sanitizer " + ann.using(), e );
        }
      }
    }

    return list;
  }

  /**
   * Internal record that binds a {@link Field} with its corresponding {@link FieldSanitizer}.
   */
  private record Holder( Field field, FieldSanitizer<Object> sanitizer ) {
  }
}
