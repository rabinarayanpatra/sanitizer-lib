package io.github.rabinarayanpatra.sanitizer.spring.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.UpperCaseSanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanitizerModuleRecordIntegrationTest {

	@Test
	void deserializeRecord_sanitizesComponents() throws Exception {
		final ObjectMapper mapper = new ObjectMapper().registerModule(new SanitizerModule());
		final RecordDto dto = mapper.readValue("{\"value\":\"  hi  \"}", RecordDto.class);
		assertEquals("HI", dto.value());
	}

	@Test
	void deserializeNestedRecordWithCascade_sanitizesNested() throws Exception {
		final ObjectMapper mapper = new ObjectMapper().registerModule(new SanitizerModule());
		final OuterDto dto = mapper.readValue("{\"inner\":{\"value\":\"  hi  \"}}", OuterDto.class);
		assertEquals("HI", dto.inner().value());
	}

	record RecordDto(@Sanitize(using = {
			TrimSanitizer.class, UpperCaseSanitizer.class}) String value) {
	}

	record OuterDto(@Sanitize(cascade = true) RecordDto inner) {
	}
}
