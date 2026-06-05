# Sanitizer-Lib

[![codecov](https://codecov.io/gh/rabinarayanpatra/sanitizer-lib/branch/master/graph/badge.svg)](https://codecov.io/gh/rabinarayanpatra/sanitizer-lib)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.rabinarayanpatra.sanitizer/sanitizer-core.svg)](https://central.sonatype.com/namespace/io.github.rabinarayanpatra.sanitizer)
[![Javadoc](https://img.shields.io/badge/javadoc-online-blue.svg)](https://rabinarayanpatra.github.io/sanitizer-lib/javadoc/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java 21](https://img.shields.io/badge/Java-21-blue.svg)](https://openjdk.org/projects/jdk/21/)

## Overview

Sanitizer-Lib is an enterprise-grade input sanitization framework for Java applications that provides comprehensive integration with Spring Boot and JPA. The library enforces data integrity and consistency by implementing automatic field sanitization on DTOs and entities prior to processing or persistence.

## Core Capabilities

### Annotations & API

- **`@Sanitize(using = Class<? extends FieldSanitizer>[])`**: Declarative annotation for field-level sanitization
- **`FieldSanitizer<T>`**: Core interface for implementing custom sanitization logic

### Built-in Sanitizers

| Sanitizer | Purpose |
|-----------|---------|
| `TrimSanitizer` | Eliminates leading and trailing whitespace |
| `LowerCaseSanitizer` | Normalizes text to lowercase |
| `UpperCaseSanitizer` | Normalizes text to uppercase |
| `TitleCaseSanitizer` | Capitalizes the first character of each word |
| `SentenceCaseSanitizer` | Capitalizes only the first character, lowercases the rest |
| `CollapseWhitespaceSanitizer` | Trims and collapses internal whitespace to a single space |
| `CreditCardMaskSanitizer` | Secures card numbers by displaying only the last four digits |
| `SSNMaskSanitizer` | Masks U.S. SSN, revealing only the last four digits |
| `IBANMaskSanitizer` | Masks IBAN except the last four characters |
| `EmailAliasStripSanitizer` | Removes `+alias` from email addresses and lowercases |
| `HtmlEscapeSanitizer` | Escapes HTML special characters |
| `SlugifySanitizer` | Converts text to a URL-friendly slug |
| `SafeFilenameSanitizer` | Replaces filesystem-reserved characters with underscores |
| `PhoneE164Sanitizer` | Normalizes phone numbers to E.164 format |
| `UuidNormalizeSanitizer` | Lowercases and validates UUID strings |
| `NullIfBlankSanitizer` | Returns null if the string is blank |
| `RemoveNonPrintableSanitizer` | Filters out non-printable control characters |
| `TruncateSanitizer` | Configurable truncation with optional suffix (new in 1.1.0) |

### Framework Integration

- **Spring Boot** (`sanitizer-spring`)
  - Jackson `SanitizerModule` for automatic sanitization during deserialization
  - Spring-managed `SanitizerRegistry` for programmatic sanitizer resolution
  - Zero-configuration setup via Spring Boot autoconfiguration

- **JPA** (`sanitizer-jpa`)
  - `SanitizationEntityListener` automatically invoked during `@PrePersist` and `@PreUpdate` lifecycle events
  - Seamless integration with existing entity models

- **JPA + Spring Boot starter** (`sanitizer-jpa-spring`)
  - Spring Boot starter wiring Hibernate-aware safety checks into the JPA entity listener (skips lazy associations)

### Architecture

The library follows a modular design with clear separation of concerns:

| Module | Responsibility |
|--------|----------------|
| **sanitizer-core** | Core API, annotations, utilities, and standard sanitizers |
| **sanitizer-spring** | Spring Boot integration with autoconfiguration support |
| **sanitizer-jpa** | JPA entity lifecycle integration |
| **sanitizer-jpa-spring** | Spring Boot starter wiring Hibernate-aware safety checks into the JPA entity listener (skips lazy associations) |

## Technical Requirements

- Java 21 (LTS)
- Gradle 8.x+
- Spring Boot 3.4.x (for Spring integration)

## Implementation Guide

### Dependency Configuration

#### Spring Boot Integration

```xml
<dependency>
    <groupId>io.github.rabinarayanpatra.sanitizer</groupId>
    <artifactId>sanitizer-spring</artifactId>
    <version>1.2.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.github.rabinarayanpatra.sanitizer:sanitizer-spring:1.2.0")
```

#### JPA Integration (Optional)

```xml
<dependency>
    <groupId>io.github.rabinarayanpatra.sanitizer</groupId>
    <artifactId>sanitizer-jpa</artifactId>
    <version>1.2.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.github.rabinarayanpatra.sanitizer:sanitizer-jpa:1.2.0")
```

#### JPA + Spring Boot Starter (Optional)

Spring Boot starter wiring Hibernate-aware safety checks into the JPA entity listener (skips lazy associations).

```xml
<dependency>
    <groupId>io.github.rabinarayanpatra.sanitizer</groupId>
    <artifactId>sanitizer-jpa-spring</artifactId>
    <version>1.2.0</version>
</dependency>
```

**Gradle (Kotlin DSL):**
```kotlin
implementation("io.github.rabinarayanpatra.sanitizer:sanitizer-jpa-spring:1.2.0")
```

## Implementation Examples

### REST API DTOs (Spring Boot)

```java
package com.example.dto;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;

public class UserDto {
    @Sanitize(using = {TrimSanitizer.class, LowerCaseSanitizer.class})
    private String email;

    // Standard getters and setters
}
```

The framework automatically applies the sanitization chain (trim, then lowercase) when Spring deserializes JSON payloads into the `UserDto` object.

### Persistent Entities (JPA)

```java
package com.example.entity;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.CreditCardMaskSanitizer;
import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;
import jakarta.persistence.*;

@Entity
@EntityListeners(SanitizationEntityListener.class)
public class Payment {
    @Id
    @GeneratedValue
    private Long id;

    @Sanitize(using = CreditCardMaskSanitizer.class)
    private String cardNumber;

    // Standard getters and setters
}
```

When the entity is persisted or updated, the framework automatically masks the credit card number, enhancing security compliance.

## Configurable Sanitizers (1.1.0+)

Sanitizers can accept parameters via the `params` attribute. Extend `ConfigurableFieldSanitizer` to create parameterized sanitizers:

```java
@Sanitize(using = TruncateSanitizer.class, params = "maxLength=100,suffix=...")
private String description;
```

Create your own configurable sanitizer:

```java
public class MaskSanitizer extends ConfigurableFieldSanitizer<String> {
    @Override
    public String sanitize(final String input) {
        if (input == null) return null;
        int reveal = getIntParam("reveal", 4);
        String ch = getParam("character", "*");
        if (input.length() <= reveal) return input;
        return ch.repeat(input.length() - reveal) + input.substring(input.length() - reveal);
    }
}
```

Usage: `@Sanitize(using = MaskSanitizer.class, params = "reveal=6,character=X")`

## Recursive sanitization (1.2.0+)

Opt in to recursive traversal of nested objects, records, collections, and maps with `@Sanitize(cascade = true)`. Java records require a different entry point because they are immutable: `SanitizationUtils.applyAndReturn(T)` returns a new instance with sanitized component values.

```java
record OrderDto(
        @Sanitize(using = TrimSanitizer.class) String reference,
        @Sanitize(cascade = true) CustomerDto customer,
        @Sanitize(cascade = true) List<LineItem> items) {}

// Records: must use applyAndReturn — it returns a new instance.
OrderDto sanitized = SanitizationUtils.applyAndReturn(rawOrder);

// POJOs: apply mutates in place; applyAndReturn also works and returns
// the same reference.
SanitizationUtils.apply(somePojo);
```

Cascade is strictly opt-in in v1.2.0. The default flips on in v2.0.0 alongside a new `@SanitizeIgnore` opt-out.

## Extending the Framework

### Custom Sanitizer Implementation

1. **Create a Sanitizer Implementation**:

```java
package com.yourorg.sanitizer;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import org.springframework.stereotype.Component;

@Component
public class NumericOnlySanitizer implements FieldSanitizer<String> {
    @Override
    public String sanitize(final String input) {
        return input == null ? null : input.replaceAll("[^0-9]", "");
    }
}
```

2. **Apply to Domain Fields**:

```java
@Sanitize(using = NumericOnlySanitizer.class)
private String phoneNumber;
```

3. **Programmatic Usage** (when needed):

```java
@Autowired
private SanitizerRegistry registry;

public String processInput(String rawPhoneNumber) {
    return registry.get(NumericOnlySanitizer.class).sanitize(rawPhoneNumber);
}
```

## Project Architecture

```
sanitizer-lib/             ← Parent project (packaging=pom)
├── sanitizer-core/        ← Core API and standard implementations
├── sanitizer-spring/      ← Spring Boot integration components
├── sanitizer-jpa/         ← JPA persistence integration
└── sanitizer-jpa-spring/  ← Spring Boot starter wiring Hibernate-aware safety checks into the JPA entity listener
```

Each module maintains its own dependency set while inheriting common configuration from the parent build script.

## Documentation

- **API Javadoc**: [https://rabinarayanpatra.github.io/sanitizer-lib/javadoc/](https://rabinarayanpatra.github.io/sanitizer-lib/javadoc/)
- **Changelog**: See [CHANGELOG.md](CHANGELOG.md)

## Release Management

- **Version Strategy**: Adheres to [Semantic Versioning](https://semver.org/) principles
- **Artifact Publishing**: Automated deployment to Maven Central via GitHub Actions
- **Javadoc Publishing**: Automated deployment to GitHub Pages on release
- **CI/CD Pipeline**: See `.github/workflows/release.yml` for implementation details

## Contribution Guidelines

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/enhancement-name`)
3. Implement changes with appropriate test coverage
4. Commit using conventional commits (`git commit -m 'feat: enhancement description'`)
5. Push changes (`git push origin feature/enhancement-name`)
6. Submit a pull request

All contributions must adhere to the established code style and include suitable test coverage.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.md) file for details.
