package io.github.rabinarayanpatra.sanitizer.spring.registry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;

@Component
public class SanitizerRegistry {

  private final Map<Class<? extends FieldSanitizer<?>>, FieldSanitizer<?>> registry = new ConcurrentHashMap<>();

  @Autowired
  public SanitizerRegistry( List<FieldSanitizer<?>> sanitizers ) {
    for( FieldSanitizer<?> s : sanitizers ) {
      @SuppressWarnings( "unchecked" ) Class<? extends FieldSanitizer<?>> cls
          = (Class<? extends FieldSanitizer<?>>) s.getClass();
      registry.put( cls, s );
    }
  }

  @SuppressWarnings( "unchecked" )
  public <T> FieldSanitizer<T> get( Class<? extends FieldSanitizer<T>> cls ) {
    FieldSanitizer<?> s = registry.get( cls );
    if( s == null ) {
      throw new IllegalArgumentException( "No sanitizer registered for " + cls.getName() );
    }
    return (FieldSanitizer<T>) s;
  }
}

