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

	@Test
	void jsonDeserialization_doesNotThrowForRecord() throws Exception {
		// A plain record (no @Sanitize) must deserialize without the module
		// throwing UnsupportedOperationException. Records are silently skipped.
		final String json = "{\"email\":\"  USER@EXAMPLE.COM  \"}";
		final SampleRecord rec = mapper.readValue(json, SampleRecord.class);
		// Record components are final and cannot be sanitized: value passes through.
		assertEquals("  USER@EXAMPLE.COM  ", rec.email());
	}

	@Test
	void jsonDeserialization_doesNotThrowForBeanEmbeddingRecord() throws Exception {
		// A POJO whose graph embeds a record must still deserialize. The POJO's own
		// annotated field is sanitized; the embedded record is skipped silently.
		final String json = "{\"outer\":\"  OUTER  \",\"record\":{\"email\":\"  USER@EXAMPLE.COM  \"}}";
		final RecordWrapperDto dto = mapper.readValue(json, RecordWrapperDto.class);
		assertEquals("outer", dto.getOuter());
		assertEquals("  USER@EXAMPLE.COM  ", dto.getRecord().email());
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

	record SampleRecord(@Sanitize(using = {
			TrimSanitizer.class, LowerCaseSanitizer.class}) String email) {
	}

	static class RecordWrapperDto {
		@Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
		private String outer;

		private SampleRecord record;

		public String getOuter() {
			return outer;
		}

		public void setOuter(final String outer) {
			this.outer = outer;
		}

		public SampleRecord getRecord() {
			return record;
		}

		public void setRecord(final SampleRecord record) {
			this.record = record;
		}
	}
}
