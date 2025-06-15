# KustomTrace

KustomTrace is a tool designed to analyze Kustomize directory structures, build a dependency graph of your Kustomize applications and resources, and provide insights into their relationships. It offers both a Java library for programmatic access and a command-line interface (CLI) for direct use.

## Key Features

* **Kustomize Graph Building:** Parses Kustomize files (`kustomization.yaml`, `kustomization.yml`, `Kustomization`) and resource files (`.yaml`, `.yml`, `.json`) to construct a comprehensive dependency graph.
* **Dependency Analysis:** Understand which applications depend on specific base kustomizations or resource files.
* **Impact Analysis:** Identify which applications are affected by changes to a given file.
* **Root Application Discovery:** Find Kustomize entry points (root applications) within your structure.
* **Java Library:** Integrate Kustomize analysis directly into your Java projects.
* **Command-Line Interface (CLI):** Perform analysis and get formatted output (human-readable or YAML) directly from your terminal or scripts.

## Modules

KustomTrace is composed of two main modules:

* **[KustomTrace Library (`lib/README.md`)](./lib/README.md):** For developers looking to use KustomTrace programmatically within their Java applications.
* **[KustomTrace CLI (`cli/README.md`)](./cli/README.md):** For users who want to use KustomTrace as a standalone command-line tool.

## High-Level Use Cases

* **CI/CD Integration:** Automatically determine which applications are impacted by code changes in a pull request to trigger targeted builds, tests, or deployments.
* **Repository Auditing:** Understand the structure and inter-dependencies of your Kustomize-managed applications.
* **Impact Assessment:** Before making changes to shared bases or resources, identify all potentially affected applications.
* **Code Cleanup:** Identify orphaned Kustomizations or unused resources.

## Real-World Demonstration: Solving CI/CD Bottlenecks

This tool was designed to solve common performance and complexity challenges in large-scale GitOps repositories. Its power is best demonstrated when combined with other tools to create intelligent CI/CD pipelines.

➡️ **[Kustomize and Kyverno at Scale: A CI/CD Demonstration](https://github.com/zucca-devops-tooling/kustomize-at-scale-demo)**

This demo repository showcases how `kustom-trace` can be used to:
* **Reliably Discover Build Targets:** Use the `list-root-apps` command to automatically find all buildable applications, eliminating manual lists.
* **Enable Efficient PR Validation:** Use the `affected-apps` command to find the exact set of applications impacted by a pull request's file changes.
* **Dramatically Speed Up Policy Enforcement:** By identifying only the affected applications, `kustom-trace` provides the precise input needed for tools like Kyverno, preventing slow "apply all" validation steps and enabling high-performance, parallel policy checks. The demo shows a **55% reduction** in policy enforcement time by moving from a monolithic to a targeted, parallel approach.

## Getting Started

### Using the CLI

The recommended way to use the KustomTrace CLI is to download the executable JAR from our official **[GitHub Releases page](https://github.com/zucca-devops-tooling/kustom-trace/releases)**.

Once there:
1.  Choose the version you want to use (e.g., the latest stable release).
2.  Download the `kustomtrace-cli-VERSION-all.jar` (or similarly named JAR that includes all dependencies).
3.  You can then run the CLI from your terminal.

**Currently, an executable JAR is provided (requires Java 17+). Native binaries for direct execution without needing a Java installation are planned for future releases.**

See the [KustomTrace CLI README](./cli/README.md) for detailed command usage.

### Using the Library

To use KustomTrace as a Java library, add it as a dependency to your project.

**(Example - Maven)**
```xml
<dependency>
    <groupId>dev.zucca-ops</groupId>
    <artifactId>kustomtrace</artifactId> <version>1.0.1</version>
</dependency>
```


**(Example - Gradle)**
```gradle
implementation 'dev.zucca-ops:kustomtrace:1.0.1'
```


See the [KustomTrace Library README](./lib/README.md) for more details on programmatic usage.

## Limitations and Future Work

### Current Limitations
* **Supported Kustomize Fields for Reference Extraction:** KustomTrace currently parses and resolves dependencies from the following standard Kustomize fields:
    * `resources`
    * `bases` (treated as kustomizations)
    * `components` (treated as kustomizations)
    * `patches` (expecting a `path` key within list items for patch files)
    * `patchesStrategicMerge` (values are paths to resource files)
    * `configMapGenerator` (parsing `envs` and `files` sub-fields for file references)
    Support for additional Kustomize fields that can reference other files (e.g., `crds`, `openapi`, certain generator options pointing to files, Helm chart values files) may be added in future versions.

### Future Development Ideas
* **User-Selectable App Path Representation (CLI):** Introduce an option (e.g., `--represent-app-as <folder|file>`) to allow users to choose whether application paths in CLI output refer to the application's directory (folder) or its specific kustomization file.
* **Bulk Queries (CLI):** Enable users to execute multiple query types in a single invocation, with results consolidated into a single output file.
* **Chained Queries / Pipelined Processing (CLI):** Allow the output of one KustomTrace command to be used as (potentially filtered) input for subsequent commands, leveraging the already built graph to avoid re-parsing the filesystem multiple times.

## Contribution

Contributions to KustomTrace are welcome!

- Found a bug or have a suggestion? Please [open an issue](https://github.com/zucca-devops-tooling/kustom-trace/issues/new).
- Want to contribute code? See our detailed [Contribution Guidelines](CONTRIBUTING.md).

Thank you for helping improve KustomTrace!

## License

This project is licensed under the Apache License 2.0 - see the `LICENSE` file for details.