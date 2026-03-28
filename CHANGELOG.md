# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

## [Unreleased]
## [1.1.0] - 2026-03-28
### Added
- Native CLI build and release support for Linux, Windows, and macOS via GraalVM Community 21 on GitHub Actions.
- Pre-release validation for the native CLI binaries in CI across Linux, Windows, and macOS.
- Support for `secretGenerator` file and env references when building Kustomize dependency graphs.

### Changed
- Upgraded the Gradle wrapper to 8.14.4.

### Fixed
- Replaced the previous concurrent graph-building approach with safer per-path locking and thread-safe dependent tracking.

### Security
- Upgraded `logback-classic` to 1.5.32.

## [1.0.1] - 2025-05-27
### 🐞 Fixed
- resources, bases and components reference validations were too flexible allowing to reference Kustomization files
- allowing Kustomization class to have other kinds than Kustomization (e.g. Component)

## [1.0.0] - 2025-05-25
### ✨ Added
- Initial release

[Unreleased]: https://github.com/zucca-devops-tooling/kustom-trace/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/zucca-devops-tooling/kustom-trace/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/zucca-devops-tooling/kustom-trace/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/zucca-devops-tooling/kustom-trace/releases/tag/v1.0.0
