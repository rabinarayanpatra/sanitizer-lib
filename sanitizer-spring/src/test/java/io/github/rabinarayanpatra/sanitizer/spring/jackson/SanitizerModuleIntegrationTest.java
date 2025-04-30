package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.rabinarayanpatra.sanitizer.annotation.SanitizeField;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest( classes = SanitizerModuleIntegrationTest.TestConfig.class )
class SanitizerModuleIntegrationTest {

  @Autowired
  ObjectMapper mapper;

  @Test
  void jsonDeserialization_appliesSanitizer() throws Exception {
    String json = "{\"email\":\"  USER@EXAMPLE.COM  \"}";
    SampleDto dto = mapper.readValue( json, SampleDto.class );
    assertEquals( "user@example.com", dto.getEmail() );
  }

  @Configuration
  @EnableAutoConfiguration
  @ComponentScan( basePackages = "io.github.rabinarayanpatra.sanitizer" )
  static class TestConfig {
    // Enables auto-config + picks up our @Component sanitizers,
    // the Jackson module via spring.factories, and the core classes.
  }

  static class SampleDto {
    @SanitizeField( using = TrimSanitizer.class )
    @SanitizeField( using = LowerCaseSanitizer.class )
    private String email;

    public String getEmail() {
      return email;
    }

    public void setEmail( String email ) {
      this.email = email;
    }
  }
}
