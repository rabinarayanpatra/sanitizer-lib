package io.github.rabinarayanpatra.sanitizer.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.rabinarayanpatra.sanitizer.annotation.SanitizeField;

public class SanitizationUtils {
  private static final Map<Class<?>, List<Holder>> CACHE = new ConcurrentHashMap<>();

  public static void apply( Object bean ) {
    if( bean == null )
      return;
    Class<?> cls = bean.getClass();
    List<Holder> holders = CACHE.computeIfAbsent( cls, SanitizationUtils::inspect );
    for( Holder h : holders ) {
      try {
        Field f = h.field;
        Object raw = f.get( bean );
        Object clean = h.sanitizer.sanitize( raw );
        f.set( bean, clean );
      } catch( IllegalAccessException ignored ) {
      }
    }
  }

  private static List<Holder> inspect( Class<?> cls ) {
    List<Holder> list = new ArrayList<>();
    for( Field field : cls.getDeclaredFields() ) {
      SanitizeField[] annotations = field.getAnnotationsByType( SanitizeField.class );
      if( annotations.length == 0 )
        continue;
      field.setAccessible( true );
      for( SanitizeField ann : annotations ) {
        try {
          // cast every sanitizer to FieldSanitizer<Object>
          @SuppressWarnings( "unchecked" ) FieldSanitizer<Object> sanitizer = (FieldSanitizer<Object>) ann.using()
              .getDeclaredConstructor().newInstance();
          list.add( new Holder( field, sanitizer ) );
        } catch( ReflectiveOperationException e ) {
          throw new RuntimeException( "Cannot instantiate sanitizer " + ann.using(), e );
        }
      }
    }
    return list;
  }

  private record Holder( Field field, FieldSanitizer<Object> sanitizer ) {
  }
}
