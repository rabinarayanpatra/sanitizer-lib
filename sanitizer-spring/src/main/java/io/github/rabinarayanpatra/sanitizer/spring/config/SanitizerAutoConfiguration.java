package io.github.rabinarayanpatra.sanitizer.spring.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;

import io.github.rabinarayanpatra.sanitizer.spring.jackson.SanitizerModule;

@Configuration
public class SanitizerAutoConfiguration {

  /**
   * Registers our Jackson module so that Spring Boot's ObjectMappers automatically apply @SanitizeField during JSON
   * deserialization.
   */
  @Bean
  public Module sanitizerModule() {
    return new SanitizerModule();
  }
}
