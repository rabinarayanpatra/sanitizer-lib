package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.spring.jackson.SanitizerModuleIntegrationTest.TestConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = TestConfig.class)
class SanitizerModuleIntegrationTest {

	@Autowired
	ObjectMapper mapper;

	@Test
	void jsonDeserialization_appliesSanitizer() throws Exception {
		final String json = "{\"email\":\"  USER@EXAMPLE.COM  \"}";
		final SampleDto dto = mapper.readValue(json, SampleDto.class);
		assertEquals("user@example.com", dto.getEmail());
	}

	@Configuration
	@EnableAutoConfiguration
	@ComponentScan(basePackages = "io.github.rabinarayanpatra.sanitizer")
	static class TestConfig {
		// Enables auto-config + picks up our @Component sanitizers,
		// the Jackson module via spring.factories, and the core classes.
	}

	static class SampleDto {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		private String email;

		public String getEmail() {
			return email;
		}

		public void setEmail(final String email) {
			this.email = email;
		}
	}
}
