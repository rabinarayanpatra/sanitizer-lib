package io.github.rabinarayanpatra.sanitizer.spring.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.Module;

import io.github.rabinarayanpatra.sanitizer.spring.config.SanitizerAutoConfigurationTest.TestConfig;
import io.github.rabinarayanpatra.sanitizer.spring.registry.SanitizerRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = TestConfig.class)
class SanitizerAutoConfigurationTest {

	@Autowired
	ApplicationContext context;

	@Test
	void registryBeanIsPresent() {
		assertNotNull(context.getBean(SanitizerRegistry.class));
	}

	@Test
	void sanitizerModuleBeanIsPresent() {
		assertNotNull(context.getBean("sanitizerModule", Module.class));
	}

	@Test
	void allBuiltinSanitizerBeansArePresent() {
		final String[] expectedBeans = {"trimSanitizer", "lowerCaseSanitizer", "upperCaseSanitizer",
				"titleCaseSanitizer", "creditCardMaskSanitizer", "ssnMaskSanitizer", "ibanMaskSanitizer",
				"htmlEscapeSanitizer", "slugifySanitizer", "phoneE164Sanitizer", "safeFilenameSanitizer",
				"collapseWhitespaceSanitizer", "nullIfBlankSanitizer", "emailAliasStripSanitizer",
				"uuidNormalizeSanitizer"};

		for (final String beanName : expectedBeans) {
			assertTrue(context.containsBean(beanName), "Missing bean: " + beanName);
		}
	}

	@Configuration
	@EnableAutoConfiguration
	@ComponentScan(basePackages = "io.github.rabinarayanpatra.sanitizer")
	static class TestConfig {
	}
}
