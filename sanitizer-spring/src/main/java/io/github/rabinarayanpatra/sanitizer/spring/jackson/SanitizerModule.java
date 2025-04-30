package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;

import io.github.rabinarayanpatra.sanitizer.core.SanitizationUtils;

public class SanitizerModule extends SimpleModule {

  public SanitizerModule() {
    super( "SanitizerModule" );
    setDeserializerModifier( new BeanDeserializerModifier() {
      @Override
      public JsonDeserializer<?> modifyDeserializer( DeserializationConfig config, BeanDescription beanDesc,
          JsonDeserializer<?> deserializer ) {
        return new StdDeserializer<>( beanDesc.getBeanClass() ) {
          @Override
          public Object deserialize( JsonParser p, DeserializationContext ctxt ) throws IOException {
            Object bean = deserializer.deserialize( p, ctxt );
            SanitizationUtils.apply( bean );
            return bean;
          }
        };
      }
    } );
  }
}
