package io.github.rabinarayanpatra.sanitizer.spring.registry;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest( classes = RegistryTest.TestConfig.class )
class RegistryTest {

  @Autowired
  SanitizerRegistry registry;

  @Test
  void lowerCaseSanitizerIsAvailable() {
    var s = registry.get( LowerCaseSanitizer.class );
    assertEquals( "foo", s.sanitize( "FOO" ) );
  }

  /**
   * Minimal configuration for Spring Boot. - @EnableAutoConfiguration to pick up our auto‐config - @ComponentScan to
   * find the Registry and any @Component sanitizers
   */
  @Configuration
  @EnableAutoConfiguration
  @ComponentScan( basePackages = "io.github.rabinarayanpatra.sanitizer" )
  static class TestConfig {
  }
}
