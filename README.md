# Sanitizer-Lib

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
| `TitleCaseSanitizer` | Capitalizes the first character of text |
| `CreditCardMaskSanitizer` | Secures card numbers by displaying only the last four digits |

### Framework Integration

- **Spring Boot** (`sanitizer-spring`)
  - Jackson `SanitizerModule` for automatic sanitization during deserialization
  - Spring-managed `SanitizerRegistry` for programmatic sanitizer resolution
  - Zero-configuration setup via Spring Boot autoconfiguration

- **JPA** (`sanitizer-jpa`)
  - `SanitizationEntityListener` automatically invoked during `@PrePersist` and `@PreUpdate` lifecycle events
  - Seamless integration with existing entity models

### Architecture

The library follows a modular design with clear separation of concerns:

| Module | Responsibility |
|--------|----------------|
| **sanitizer-core** | Core API, annotations, utilities, and standard sanitizers |
| **sanitizer-spring** | Spring Boot integration with autoconfiguration support |
| **sanitizer-jpa** | JPA entity lifecycle integration |

## Technical Requirements

- Java 21 (LTS)
- Maven 3.8+
- Spring Boot 3.4.x (for Spring integration)

## Implementation Guide

### Dependency Configuration

#### Spring Boot Integration

```xml
<dependency>
    <groupId>io.github.rabinarayanpatra.sanitizer</groupId>
    <artifactId>sanitizer-spring</artifactId>
    <version>1.0.15</version>
</dependency>
```

#### JPA Integration (Optional)

```xml
<dependency>
    <groupId>io.github.rabinarayanpatra.sanitizer</groupId>
    <artifactId>sanitizer-jpa</artifactId>
    <version>1.0.15</version>
</dependency>
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
└── sanitizer-jpa/         ← JPA persistence integration
```

Each module maintains its own dependency set while inheriting common configuration from the parent POM.

## Release Management

- **Version Strategy**: Adheres to [Semantic Versioning](https://semver.org/) principles
- **Artifact Publishing**: Automated deployment to Maven Central via GitHub Actions
- **CI/CD Pipeline**: See `.github/workflows/maven-publish.yml` for implementation details

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
