# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.0.23] - 2026-04-01

### Fixed
- `@Sanitize` is now `@Repeatable` — multiple annotations can be stacked on the same field
- `SanitizationUtils.inspect()` now walks the class hierarchy, supporting `@MappedSuperclass` and inherited fields
- `SanitizationUtils.apply()` catches `ClassCastException` with a diagnostic message when a sanitizer's type doesn't match the field type
- `TitleCaseSanitizer` now correctly capitalizes the first letter of each word (was only capitalizing the first character)
- `SanitizerInstantiationException` now has `serialVersionUID`
- README version updated from 1.0.21 to 1.0.23
- README now correctly states Gradle (was Maven) in Technical Requirements
- `@Sanitize` Javadoc now documents ordering guarantee

### Changed
- `HtmlEscapeSanitizer` Javadoc now includes a security note clarifying it is not an XSS prevention control

### Added
- `@Sanitizes` container annotation for `@Repeatable` support

## [1.0.22] - 2025-xx-xx

### Fixed
- `IllegalAccessException` in `SanitizationUtils.apply()` now throws `IllegalStateException` with diagnostic info instead of being silently swallowed

### Added
- `SanitizationUtils` unit tests
- `RemoveNonPrintableSanitizer` for filtering non-printable control characters

## [1.0.0] - [1.0.21]

Initial releases. Core API with 16 built-in sanitizers, Spring Boot autoconfiguration, JPA entity listener, Jackson deserialization module. Published to Maven Central.
