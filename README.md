# Sanitizer-Lib

**Sanitizer-Lib** is a production-grade, extensible input sanitization library for Java applications, with first-class
support for Spring Boot (JSON deserialization) and JPA (entity persistence). It helps you enforce data cleanliness and
consistency by automatically cleaning up fields on DTOs and entities before storage or processing.

---

## Features

- **Core Annotations & API**
    - `@Sanitize(using = YourSanitizer.class)` to declare sanitization on individual fields.
    - `FieldSanitizer<T>` interface for custom sanitizers.
    - Built-in sanitizers:
        - `TrimSanitizer` (removes leading/trailing whitespace)
        - `LowerCaseSanitizer` (converts strings to lowercase)
        - `TitleCaseSanitizer` (capitalizes first character)
        - `CreditCardMaskSanitizer` (masks all but last 4 digits of card numbers)
- **Spring Boot Integration** (`sanitizer-spring`)
    - Jackson `SanitizerModule` auto-applies sanitization on JSON→DTO binding.
    - `SanitizerRegistry` for programmatic lookup of sanitizers as Spring beans.
    - Auto-configuration via `spring.factories` – just add the starter to your classpath.
- **JPA Integration** (`sanitizer-jpa`)
    - `SanitizationEntityListener` hooks into `@PrePersist` & `@PreUpdate` to sanitize entities automatically.
- **Extensible & Configurable**
    - Implement `FieldSanitizer<T>` and annotate fields, or register your own beans.
    - Fully annotation-driven; no XML or manual wiring required.

---

## Modules

| Module               | Purpose                                             |
|----------------------|-----------------------------------------------------|
| **sanitizer-core**   | Core API, annotations, utils & built-in sanitizers. |
| **sanitizer-spring** | Spring Boot auto-config & Jackson integration.      |
| **sanitizer-jpa**    | JPA entity listener for database sanitization.      |

---

## Getting Started

### Requirements

- Java 21 (LTS)
- Maven 3.8+
- (Optional) Spring Boot 3.4.x if using Spring integration

### Add Dependency

```xml

<dependency>
    <groupId>io.github.rabinarayanpatra.sanitizer</groupId>
    <artifactId>sanitizer-spring</artifactId>
    <version>1.0.0</version>
</dependency>
```

If you need JPA support, also add:

```xml

<dependency>
    <groupId>io.github.rabinarayanpatra.sanitizer</groupId>
    <artifactId>sanitizer-jpa</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## Usage Examples

### JSON Deserialization (Spring MVC / WebFlux)

```java
package com.example.dto;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.TrimSanitizer;
import io.github.rabinarayanpatra.sanitizer.builtin.LowerCaseSanitizer;

public class UserDto {
  @Sanitize( using = { TrimSanitizer.class, LowerCaseSanitizer.class } )
  private String email;

  // getters/setters...
}
```

When Spring Boot deserializes incoming JSON into `UserDto`, the `email` field will be trimmed and lower-cased
automatically.

### JPA Entities

```java
package com.example.entity;

import io.github.rabinarayanpatra.sanitizer.annotation.Sanitize;
import io.github.rabinarayanpatra.sanitizer.builtin.CreditCardMaskSanitizer;
import io.github.rabinarayanpatra.sanitizer.jpa.SanitizationEntityListener;
import jakarta.persistence.*;

@Entity
@EntityListeners( SanitizationEntityListener.class )
public class Payment {
  @Id
  @GeneratedValue
  private Long id;

  @Sanitize( using = CreditCardMaskSanitizer.class )
  private String cardNumber;

  // getters/setters...
}
```

On `repository.save(payment)`, the `cardNumber` will be masked (all but last 4 digits) before being persisted.

---

## Creating Custom Sanitizers

1. **Implement `FieldSanitizer<T>`**:

   ```java

package com.yourorg.sanitizer;

import io.github.rabinarayanpatra.sanitizer.core.FieldSanitizer;
import org.springframework.stereotype.Component;

@Component
public class MyCustomSanitizer implements FieldSanitizer<String> {
@Override
public String sanitize( final String input) {
// Your logic here...
return input == null ? null : input.replaceAll("[^0-9]", "");
}
}

```

2. **Annotate your fields**:

    ```java
    @Sanitize(using = MyCustomSanitizer.class )
    private String rawPhoneNumber;
    ```

3. **(Optional) Programmatic lookup** via `SanitizerRegistry`:

    ```java
    @Autowired
    private SanitizerRegistry registry;

    String cleaned = registry.get(MyCustomSanitizer.class).sanitize(rawInput);
    ```

---

## Multi-Module Structure

```

sanitizer-lib/ ← Parent POM (packaging=pom)
├── sanitizer-core/ ← Core annotations, interfaces, built-ins
├── sanitizer-spring/ ← Spring Boot auto-configuration & Jackson module
└── sanitizer-jpa/ ← JPA EntityListener & converters

```

Each module is a Maven sub-module inheriting versions and dependency management from the parent.

---

## Publishing & CI/CD

- **Versioning**: Follows [Semantic Versioning](https://semver.org/). Tag releases as `v1.0.0`, `v1.1.0`, etc.
- **Maven Central**: Use GitHub Actions to `mvn deploy` on tag push with OSSRH/GPG credentials.
- **GitHub Workflow Skeleton**: see `.github/workflows/maven-publish.yml`.

---

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Commit your changes (`git commit -m 'feat: your feature'`)
4. Push to your branch (`git push origin feature/your-feature`)
5. Open a pull request

Please follow the existing coding style and add unit/integration tests for new features.

---

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.
