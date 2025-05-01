package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;

/**
 * Utility class that applies {@link FieldSanitizer} logic to bean fields annotated with {@link Sanitize}.
 *
 * @since 1.0.0
 */
public final class SanitizationUtils {

  private static final Map<Class<?>, List<Holder>> CACHE = new ConcurrentHashMap<>();

  private SanitizationUtils() {
  }

  /**
   * Applies all configured sanitizers to the fields of the given bean that are annotated with {@link Sanitize}.
   *
   * @param bean the object whose fields should be sanitized
   */
  @SuppressWarnings( "java:S108" )
  public static void apply( final Object bean ) {
    if( bean == null ) {
      return;
    }

    final Class<?> cls = bean.getClass();
    final List<Holder> holders = CACHE.computeIfAbsent( cls, SanitizationUtils::inspect );

    for( final Holder h : holders ) {
      try {
        final Field f = h.field;
        final Object raw = f.get( bean );
        final Object clean = h.sanitizer.sanitize( raw );
        f.set( bean, clean );
      } catch( final IllegalAccessException ignored ) {
      }
    }
  }

  /**
   * Inspects the class to find fields annotated with {@link Sanitize} and builds sanitizer handlers.
   */
  private static List<Holder> inspect( final Class<?> cls ) {
    final List<Holder> list = new ArrayList<>();

    for( final Field field : cls.getDeclaredFields() ) {
      final Sanitize ann = field.getAnnotation( Sanitize.class );
      if( ann == null ) {
        continue;
      }

      field.setAccessible( true );
      for( final Class<? extends FieldSanitizer<?>> sanitizerClass : ann.using() ) {
        try {
          @SuppressWarnings( "unchecked" ) final FieldSanitizer<Object> sanitizer
              = (FieldSanitizer<Object>) sanitizerClass.getDeclaredConstructor().newInstance();
          list.add( new Holder( field, sanitizer ) );
        } catch( final ReflectiveOperationException e ) {
          throw new SanitizerInstantiationException( "Cannot instantiate sanitizer " + sanitizerClass.getName(), e );
        }
      }
    }

    return list;
  }

  /**
   * Binds a {@link Field} with its corresponding {@link FieldSanitizer}.
   */
  private record Holder( Field field, FieldSanitizer<Object> sanitizer ) {
  }
}
