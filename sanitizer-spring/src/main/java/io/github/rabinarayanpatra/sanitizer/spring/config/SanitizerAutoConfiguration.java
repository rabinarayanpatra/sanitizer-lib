package io.github.rabinarayanpatra.sanitizer.spring.config;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

import com.fasterxml.jackson.databind.Module;

import io.github.rabinarayanpatra.sanitizer.builtin.*;
import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import io.github.rabinarayanpatra.sanitizer.spring.jackson.SanitizerModule;
import io.github.rabinarayanpatra.sanitizer.spring.registry.SanitizerRegistry;

/**
 * Spring Boot autoconfiguration for the Sanitizer library.
 * <p>
 * This configuration class exposes all built-in {@link FieldSanitizer}
 * implementations as Spring beans, registers a {@link SanitizerRegistry} that
 * holds them, and provides a Jackson {@link Module} for {@code @Sanitize}
 * support during JSON deserialization.
 *
 * @since 1.0.0
 */
@AutoConfiguration
public class SanitizerAutoConfiguration {

	/**
	 * Default constructor.
	 */
	public SanitizerAutoConfiguration() {
	}

	// ————————————————————————————————————————
	// 1) Register all built-in sanitizers as beans
	// ————————————————————————————————————————

	/**
	 * Creates a {@link CollapseWhitespaceSanitizer} bean.
	 *
	 * @return a {@link CollapseWhitespaceSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> collapseWhitespaceSanitizer() {
		return new CollapseWhitespaceSanitizer();
	}

	/**
	 * Creates a {@link CreditCardMaskSanitizer} bean.
	 *
	 * @return a {@link CreditCardMaskSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> creditCardMaskSanitizer() {
		return new CreditCardMaskSanitizer();
	}

	/**
	 * Creates an {@link EmailAliasStripSanitizer} bean.
	 *
	 * @return an {@link EmailAliasStripSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> emailAliasStripSanitizer() {
		return new EmailAliasStripSanitizer();
	}

	/**
	 * Creates an {@link HtmlEscapeSanitizer} bean.
	 *
	 * @return an {@link HtmlEscapeSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> htmlEscapeSanitizer() {
		return new HtmlEscapeSanitizer();
	}

	/**
	 * Creates an {@link IBANMaskSanitizer} bean.
	 *
	 * @return an {@link IBANMaskSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> ibanMaskSanitizer() {
		return new IBANMaskSanitizer();
	}

	/**
	 * Creates a {@link LowerCaseSanitizer} bean.
	 *
	 * @return a {@link LowerCaseSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> lowerCaseSanitizer() {
		return new LowerCaseSanitizer();
	}

	/**
	 * Creates a {@link NullIfBlankSanitizer} bean.
	 *
	 * @return a {@link NullIfBlankSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> nullIfBlankSanitizer() {
		return new NullIfBlankSanitizer();
	}

	/**
	 * Creates a {@link PhoneE164Sanitizer} bean.
	 *
	 * @return a {@link PhoneE164Sanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> phoneE164Sanitizer() {
		return new PhoneE164Sanitizer();
	}

	/**
	 * Creates a {@link SafeFilenameSanitizer} bean.
	 *
	 * @return a {@link SafeFilenameSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> safeFilenameSanitizer() {
		return new SafeFilenameSanitizer();
	}

	/**
	 * Creates a {@link SlugifySanitizer} bean.
	 *
	 * @return a {@link SlugifySanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> slugifySanitizer() {
		return new SlugifySanitizer();
	}

	/**
	 * Creates an {@link SSNMaskSanitizer} bean.
	 *
	 * @return an {@link SSNMaskSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> ssnMaskSanitizer() {
		return new SSNMaskSanitizer();
	}

	/**
	 * Creates a {@link TitleCaseSanitizer} bean.
	 *
	 * @return a {@link TitleCaseSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> titleCaseSanitizer() {
		return new TitleCaseSanitizer();
	}

	/**
	 * Creates a {@link TrimSanitizer} bean.
	 *
	 * @return a {@link TrimSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> trimSanitizer() {
		return new TrimSanitizer();
	}

	/**
	 * Creates an {@link UpperCaseSanitizer} bean.
	 *
	 * @return an {@link UpperCaseSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> upperCaseSanitizer() {
		return new UpperCaseSanitizer();
	}

	/**
	 * Creates a {@link UuidNormalizeSanitizer} bean.
	 *
	 * @return a {@link UuidNormalizeSanitizer} bean
	 */
	@Bean
	public FieldSanitizer<String> uuidNormalizeSanitizer() {
		return new UuidNormalizeSanitizer();
	}

	// ————————————————————————————————————————
	// 2) Registry that aggregates all sanitizers
	// ————————————————————————————————————————

	/**
	 * Builds a registry that holds all discovered sanitizer beans.
	 *
	 * @param sanitizers
	 *            list of all registered {@link FieldSanitizer} beans
	 * @return a new {@link SanitizerRegistry}
	 */
	@Bean
	public SanitizerRegistry sanitizerRegistry(final List<FieldSanitizer<?>> sanitizers) {
		return new SanitizerRegistry(sanitizers);
	}

	// ————————————————————————————————————————
	// 3) Jackson module for @Sanitize support
	// ————————————————————————————————————————

	/**
	 * Registers a Jackson module that applies {@code @Sanitize} annotations during
	 * deserialization.
	 *
	 * @return the configured Jackson {@link Module}
	 */
	@Bean
	public Module sanitizerModule() {
		return new SanitizerModule();
	}
}
