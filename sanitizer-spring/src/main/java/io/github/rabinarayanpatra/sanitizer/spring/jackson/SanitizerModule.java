package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

public class SanitizerModule extends SimpleModule {

  public SanitizerModule() {
    super( "SanitizerModule" );
    setDeserializerModifier( new BeanDeserializerModifier() {
      @Override
      public JsonDeserializer<?> modifyDeserializer( DeserializationConfig config, BeanDescription beanDesc,
          JsonDeserializer<?> deserializer ) {
        return new SanitizingDeserializer( deserializer );
      }
    } );
  }

  /**
   * DelegatingDeserializer forwards all calls to the original, then lets us hook in after the bean is deserialized.
   */
  private static class SanitizingDeserializer extends DelegatingDeserializer {
    protected SanitizingDeserializer( JsonDeserializer<?> delegate ) {
      super( delegate );
    }

    @Override
    public Object deserialize( JsonParser p, DeserializationContext ctxt ) throws IOException {
      // first do the normal bean deserialization
      Object bean = super.deserialize( p, ctxt );
      // then apply all @SanitizeField rules
      SanitizationUtils.apply( bean );
      return bean;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance( JsonDeserializer<?> newDelegate ) {
      return new SanitizingDeserializer( newDelegate );
    }
  }
}
