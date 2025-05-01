package io.github.rabinarayanpatra.sanitizer.spring.config;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.spring.jackson.SanitizerModule;
import io.github.rabinarayanpatra.sanitizer.spring.registry.SanitizerRegistry;

/**
 * Spring Boot autoconfiguration that registers the {@link SanitizerModule} with Jackson.
 * <p>
 * This ensures that fields annotated with {@link io.github.rabinarayanpatra.sanitizer.annotation.SanitizeField} are
 * automatically sanitized during JSON deserialization in Spring-managed {@code ObjectMapper} instances.
 *
 * @since 1.0.0
 */
@Configuration
@AutoConfiguration
public class SanitizerAutoConfiguration {

  private final List<FieldSanitizer<?>> sanitizers;

  public SanitizerAutoConfiguration( final List<FieldSanitizer<?>> sanitizers ) {
    this.sanitizers = sanitizers;
  }

  /**
   * Registers the custom Jackson module for applying field sanitization during deserialization.
   *
   * @return a Jackson {@link Module} that triggers sanitization on fields annotated with {@code @SanitizeField}
   */
  @Bean
  public Module sanitizerModule() {
    return new SanitizerModule();
  }

  @Bean
  public SanitizerRegistry sanitizerRegistry() {
    return new SanitizerRegistry( sanitizers );
  }
}
