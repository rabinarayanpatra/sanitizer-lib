# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/), and this project adheres to [Semantic Versioning](https://semver.org/).

## [1.2.0] - 2026-06-06

### Added
- Java records support via new `SanitizationUtils.applyAndReturn(T)` entry point.
- Opt-in recursive traversal via `@Sanitize(cascade = true)` for nested objects, records, collections, and maps.
- `TraversalSafetyChecker` interface for pluggable graph-walk gating.
- New `sanitizer-jpa-spring` module providing `HibernateSafetyChecker` that skips non-initialized lazy JPA associations.
- SLF4J logging in the traversal engine at DEBUG/TRACE levels.

### Changed
- `@Sanitize.using()` now defaults to `{}` so a field can opt in to cascade without supplying a self-sanitizer.
- `SanitizationUtils.apply(record)` throws `IllegalArgumentException` (was `UnsupportedOperationException`) and recommends `applyAndReturn`.

### Compatibility
- All existing POJO behavior is bit-identical. Cascade is strictly opt-in in this release.
- The default cascade flag will flip on in v2.0.0 along with a new `@SanitizeIgnore` opt-out annotation.

## [1.1.0] - 2026-04-01

### Added
- **Configurable sanitizers** — `ConfigurableFieldSanitizer<T>` base class with `params` attribute on `@Sanitize` for key-value configuration
- `TruncateSanitizer` — configurable string truncation with optional suffix (`maxLength`, `suffix` params)
- `SentenceCaseSanitizer` - capitalizes only the first character and lowercases the rest
- `RemoveNonPrintableSanitizer` bean registered in Spring autoconfiguration (was missing)
- Java records detection — throws `UnsupportedOperationException` with clear guidance instead of silent failure
- GitHub Actions workflow for publishing Javadoc to GitHub Pages on release
- Dependabot configuration for automated Gradle and GitHub Actions dependency updates
- README now lists all 18 built-in sanitizers with descriptions
- README section documenting configurable sanitizer usage

### Changed
- Version bumped from 1.0.23 to 1.1.0 (minor release — new features, backward-compatible)

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
- Error Prone static analysis plugin — catches bugs at compile time
- JSpecify `@NullMarked` / `@Nullable` annotations across all public API
- `package-info.java` for all 7 packages with module-level Javadoc
- JaCoCo code coverage reporting (HTML + XML)
- Unit tests for all 16 built-in sanitizers (104 total tests across all modules)
- `SanitizerAutoConfigurationTest` — verifies all 16 beans, registry, and Jackson module
- Expanded `RegistryTest` — lookup, multiple sanitizers, unregistered sanitizer error
- All `toLowerCase()`/`toUpperCase()` calls now use `Locale.ROOT` for locale-independent behavior

## [1.0.22] - 2025-xx-xx

### Fixed
- `IllegalAccessException` in `SanitizationUtils.apply()` now throws `IllegalStateException` with diagnostic info instead of being silently swallowed

### Added
- `SanitizationUtils` unit tests
- `RemoveNonPrintableSanitizer` for filtering non-printable control characters

## [1.0.0] - [1.0.21]

Initial releases. Core API with 16 built-in sanitizers, Spring Boot autoconfiguration, JPA entity listener, Jackson deserialization module. Published to Maven Central.
