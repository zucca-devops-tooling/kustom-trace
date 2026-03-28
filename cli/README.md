# KustomTrace CLI

The CLI analyzes a Kustomize repository from the command line and can be used in local scripts, CI jobs, and release pipelines.

## Install

Download assets from the project's [GitHub Releases](https://github.com/zucca-devops-tooling/kustom-trace/releases).

Available CLI artifacts:

- Shaded JAR: `kustomtrace-cli-<version>-all.jar`
- Linux native archive: `kustomtrace-linux-x64.tar.gz`
- Windows native archive: `kustomtrace-windows-x64.zip`
- macOS native archive: `kustomtrace-macos-x64.tar.gz`

The native archives contain a single executable:

- Linux: `kustomtrace-linux-x64`
- Windows: `kustomtrace-windows-x64.exe`
- macOS: `kustomtrace-macos-x64`

Use the native binary when you want a standalone executable with no local Java dependency. Use the JAR when you already have Java 17+ available or when you are on a release that predates the native packages.

## Basic Invocation

Examples below assume the executable is available as `kustomtrace`. If you run the release asset directly, use the extracted filename instead.

```bash
kustomtrace --apps-dir ./apps list-root-apps
java -jar kustomtrace-cli-1.1.0-all.jar --apps-dir ./apps list-root-apps
```

Important: global options must appear before the subcommand.

Correct:

```bash
kustomtrace --apps-dir ./apps --output roots.yaml list-root-apps
```

Incorrect:

```bash
kustomtrace list-root-apps --apps-dir ./apps
```

## Global Options

- `-a`, `--apps-dir <dir>`: required root directory to scan
- `-o`, `--output <file>`: write YAML output to a file instead of printing the result list
- `--log-file <file>`: append warnings, errors, and application logs to a file
- `--log-level <level>`: set log level for application logging; supported values are `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`
- `-h`, `--help`: show help
- `-v`, `--version`: show version

When YAML is written, paths are normalized with forward slashes for stable cross-platform output.

## Commands

### `list-root-apps`

Lists root applications under `--apps-dir`. A root application is a Kustomization directory that is not referenced by another Kustomization in the scanned graph.

```bash
kustomtrace --apps-dir ./apps list-root-apps
kustomtrace --apps-dir ./apps --output root-apps.yaml list-root-apps
```

Example YAML output:

```yaml
---
root-apps:
  - payments/prod
  - platform/shared-base
```

### `app-files`

Lists every unique file used by an application, including the application's own kustomization file and all transitive dependencies.

Pass the application directory as the argument.

```bash
kustomtrace --apps-dir ./apps app-files ./apps/payments/prod
kustomtrace --apps-dir ./apps --output app-files.yaml app-files ./apps/payments/prod
```

Example YAML output:

```yaml
---
app-files:
  payments/prod:
    - ../../base/common/deployment.yaml
    - kustomization.yaml
    - service.yaml
```

The YAML key is the app path relative to `--apps-dir`. The listed files are relative to the application directory.

### `affected-apps`

Lists root applications affected by one or more changed files.

Pass changed files directly:

```bash
kustomtrace --apps-dir ./apps affected-apps ./apps/base/common.yaml ./apps/shared/labels.yaml
```

Or load them from a text file, one path per line:

```bash
kustomtrace --apps-dir ./apps --output affected.yaml affected-apps --files-from-file changed-files.txt
```

Example YAML output:

```yaml
---
affected-apps:
  base/common.yaml:
    - payments/prod
    - search/staging
  shared/labels.yaml:
    - payments/prod
```

If a modified file is inside `--apps-dir`, its YAML key is relative to `--apps-dir`. Otherwise the key is written from the provided path string with normalized separators.

## Logging and Output

- Without `--output`, results are printed to the console.
- With `--output`, command results are written as YAML.
- Without `--log-file`, warnings and application logs go to the console.
- With `--log-file`, warnings, errors, and application logs are appended to that file.

`--log-level` is case-insensitive and affects both the JAR and the native binaries.

## Native Binaries

The native CLI is built with GraalVM Native Image. It uses the same arguments, output format, and logging behavior as the shaded JAR.

Release packaging:

- Linux x64: `kustomtrace-linux-x64.tar.gz`
- Windows x64: `kustomtrace-windows-x64.zip`
- macOS x64: `kustomtrace-macos-x64.tar.gz`
- Checksums: matching `.sha256` files for each archive

To build a native binary locally:

```bash
./gradlew :kustomtrace-cli:nativeCompile
```

Output location:

- Unix-like systems: `cli/build/native/nativeCompile/kustomtrace`
- Windows: `cli/build/native/nativeCompile/kustomtrace.exe`

CI currently builds the native artifacts with GraalVM Community 21. For local native builds, install GraalVM with the `native-image` component.

## Build from Source

Build the shaded JAR:

```bash
./gradlew :kustomtrace-cli:shadowJar
```

Expected output:

- `cli/build/libs/kustomtrace-cli-<version>-all.jar`

On Windows, use `gradlew.bat`.
