# Contribution Guidelines

Thank you for considering contributing to **KustomTrace**! Your contributions make this project better.

## 🐞 Reporting Issues

If you've found a bug or unexpected behavior, please create an issue including:

- **Steps to reproduce**
- **Expected behavior**
- **Actual behavior**
- **Environment details**:
    - KustomTrace version
    - Java version (`java -version`)
    - Operating system and version
    - Any relevant configuration or sample files

Clear details help us resolve issues faster.

## 🚀 Contributing Code

To submit code contributions, please follow these steps:

### 1. Fork and Clone the Repository

- Fork the repository on GitHub.
- Clone your fork locally:

```bash
git clone https://github.com/zucca-devops-tooling/kustom-trace.git
cd kustom-trace
```

- Create a new branch from the latest `main` branch:

```bash
git checkout -b feature/my-improvement
```

### 2. Set Up Your Development Environment

- Ensure you have **Java 17** or higher:

```bash
java -version
```

### 3. Implement Your Changes

- Follow existing coding conventions and style.
- Include appropriate tests (unit and/or functional tests):
    - **Library unit tests:** under `lib/src/test/java/`
    - **Functional tests:** under `functional-test/src/test/java`

### 4. Run Tests and Quality Checks

Before submitting your Pull Request, ensure your changes pass tests:

- **Unit Tests:**

```bash
./gradlew :kustomtrace:test
```

- **Functional Tests:**

```bash
./gradlew :functional-test:test
```

- **Code formatting and License headers:**

```bash
./gradlew spotlessApply
./gradlew spotlessCheck
```

> **Note:** `spotlessApply` formats your code automatically.

### 5. Submit Your Pull Request

- Push your changes:

```bash
git push origin feature/my-improvement
```

- Open a Pull Request against the main repository's `main` branch.

In your PR description, include:

- **Description** of changes
- **How to test your changes**
- **Relevant context or considerations**

## 💡 Code Review and CI/CD

After submitting your Pull Request:

- The CI/CD pipeline (Jenkins) automatically builds and tests your changes.
- Keep commit messages clear and respond promptly to any feedback during reviews.

## 🛠 Coding Standards and Project Structure

- **Java version:** Java 17+
- **Build tool:** Gradle Wrapper (`./gradlew`)
- **Testing:** JUnit for unit and functional tests
- **Formatting:** Managed by Spotless (configured in the project)

All contributions are licensed under Apache License 2.0. Spotless handles file headers automatically.

## 🤝 Direct Collaboration and Questions

For major contributions or any questions about contributing, please contact the project maintainers.

---

**Thanks again for contributing!**