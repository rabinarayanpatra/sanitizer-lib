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
 * This configuration class exposes all built-in {@link FieldSanitizer} implementations as Spring beans, registers a
 * {@link SanitizerRegistry} that holds them, and provides a Jackson {@link Module} for {@code @Sanitize} support during
 * JSON deserialization.
 *
 * @since 1.0.0
 */
@AutoConfiguration
public class SanitizerAutoConfiguration {

  // ————————————————————————————————————————
  // 1) Register all built-in sanitizers as beans
  // ————————————————————————————————————————

  /**
   * @return a {@link CollapseWhitespaceSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> collapseWhitespaceSanitizer() {
    return new CollapseWhitespaceSanitizer();
  }

  /**
   * @return a {@link CreditCardMaskSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> creditCardMaskSanitizer() {
    return new CreditCardMaskSanitizer();
  }

  /**
   * @return an {@link EmailAliasStripSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> emailAliasStripSanitizer() {
    return new EmailAliasStripSanitizer();
  }

  /**
   * @return an {@link HtmlEscapeSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> htmlEscapeSanitizer() {
    return new HtmlEscapeSanitizer();
  }

  /**
   * @return an {@link IBANMaskSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> ibanMaskSanitizer() {
    return new IBANMaskSanitizer();
  }

  /**
   * @return a {@link LowerCaseSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> lowerCaseSanitizer() {
    return new LowerCaseSanitizer();
  }

  /**
   * @return a {@link NullIfBlankSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> nullIfBlankSanitizer() {
    return new NullIfBlankSanitizer();
  }

  /**
   * @return a {@link PhoneE164Sanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> phoneE164Sanitizer() {
    return new PhoneE164Sanitizer();
  }

  /**
   * @return a {@link SafeFilenameSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> safeFilenameSanitizer() {
    return new SafeFilenameSanitizer();
  }

  /**
   * @return a {@link SlugifySanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> slugifySanitizer() {
    return new SlugifySanitizer();
  }

  /**
   * @return an {@link SSNMaskSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> ssnMaskSanitizer() {
    return new SSNMaskSanitizer();
  }

  /**
   * @return a {@link TitleCaseSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> titleCaseSanitizer() {
    return new TitleCaseSanitizer();
  }

  /**
   * @return a {@link TrimSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> trimSanitizer() {
    return new TrimSanitizer();
  }

  /**
   * @return an {@link UpperCaseSanitizer} bean
   */
  @Bean
  public FieldSanitizer<String> upperCaseSanitizer() {
    return new UpperCaseSanitizer();
  }

  /**
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
   * @param sanitizers list of all registered {@link FieldSanitizer} beans
   * @return a new {@link SanitizerRegistry}
   */
  @Bean
  public SanitizerRegistry sanitizerRegistry( final List<FieldSanitizer<?>> sanitizers ) {
    return new SanitizerRegistry( sanitizers );
  }

  // ————————————————————————————————————————
  // 3) Jackson module for @Sanitize support
  // ————————————————————————————————————————

  /**
   * Registers a Jackson module that applies {@code @Sanitize} annotations during deserialization.
   *
   * @return the configured Jackson {@link Module}
   */
  @Bean
  public Module sanitizerModule() {
    return new SanitizerModule();
  }
}
