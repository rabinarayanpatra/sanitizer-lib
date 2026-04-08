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

	@Test
	void jsonDeserialization_appliesSanitizerToNestedBeans() throws Exception {
		// Nested beans force Jackson to contextualize the delegate for each property,
		// which exercises SanitizingDeserializer#newDelegatingInstance.
		final String json = "{\"outer\":\"  OUTER  \",\"nested\":{\"email\":\"  USER@EXAMPLE.COM  \"}}";
		final WrapperDto dto = mapper.readValue(json, WrapperDto.class);
		assertEquals("outer", dto.getOuter());
		assertEquals("user@example.com", dto.getNested().getEmail());
	}

	@Configuration
	@EnableAutoConfiguration
	@ComponentScan(basePackages = "io.github.rabinarayanpatra.sanitizer")
	static class TestConfig {
		// Enables auto-config + picks up our @Component sanitizers,
		// the Jackson module via spring.factories, and the core classes.
	}

	static class WrapperDto {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		private String outer;

		private SampleDto nested;

		public String getOuter() {
			return outer;
		}

		public void setOuter(final String outer) {
			this.outer = outer;
		}

		public SampleDto getNested() {
			return nested;
		}

		public void setNested(final SampleDto nested) {
			this.nested = nested;
		}
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
