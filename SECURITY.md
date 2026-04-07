# Security Policy

## Supported Versions

Security fixes are applied to the latest minor release on the current major version line. Older versions may receive fixes at the maintainer's discretion.

| Version | Supported          |
| ------- | ------------------ |
| 1.1.x   | :white_check_mark: |
| < 1.1   | :x:                |

## Reporting a Vulnerability

If you believe you have found a security vulnerability in Sanitizer-Lib, please report it privately. **Do not open a public GitHub issue.**

Please use one of the following channels:

- **GitHub Security Advisories** (preferred): [Open a private advisory](https://github.com/rabinarayanpatra/sanitizer-lib/security/advisories/new)
- **Email**: contact the maintainer via the email listed on the [GitHub profile](https://github.com/rabinarayanpatra)

When reporting, please include:

- A clear description of the vulnerability and its impact
- Steps to reproduce, including affected version(s) and configuration
- A minimal proof-of-concept if possible
- Any suggested mitigation or fix

## Response Process

- You will receive an acknowledgement within **5 business days** of your report.
- The maintainer will investigate and confirm the issue, then work on a fix.
- A coordinated disclosure timeline will be agreed with the reporter.
- Once a fix is released, a security advisory will be published on GitHub crediting the reporter (unless anonymity is requested).

## Scope

In scope:

- The `sanitizer-core`, `sanitizer-spring`, and `sanitizer-jpa` modules published under `io.github.rabinarayanpatra.sanitizer` on Maven Central
- The library's built-in sanitizers and their handling of untrusted input
- Misuse-resistant defaults of the `@Sanitize` annotation and registry

Out of scope:

- Vulnerabilities in third-party dependencies (please report those upstream)
- Issues that require attacker control of the application's source code
- Denial-of-service caused by passing pathological input to a custom user-defined sanitizer

Thank you for helping keep Sanitizer-Lib and its users safe.
