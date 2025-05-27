# KustomTrace CLI

The KustomTrace Command-Line Interface (CLI) allows you to analyze Kustomize directory structures and dependencies directly from your terminal or in scripts.

## Installation

1.  Go to the **[KustomTrace GitHub Releases page](https://github.com/zucca-devops-tooling/kustom-trace/releases)**.
2.  Select the desired release version (e.g., the latest stable release).
3.  From the "Assets" section of that release, download the `kustomtrace-cli-VERSION-all.jar` file (this is an executable "fat JAR" containing all dependencies).

**Requirements:**
* Java 17 (or newer) is required to run the JAR.

*(Native binaries for direct execution without needing a Java installation are planned for future releases.)*

## Basic Invocation
```bash
java -jar /path/to/your/downloaded/kustomtrace-cli-VERSION-all.jar [GLOBAL_OPTIONS] <subcommand> [SUBCOMMAND_OPTIONS_ARGS]
```

For easier use, you might want to:
* Place the JAR in a directory that's in your system's `PATH`.
* Create a shell alias or a small wrapper script (e.g., `kustomtrace`) to simplify the invocation.

**Example:**
```bash
java -jar kustomtrace-cli-0.1.0-all.jar list-root-apps -a ./my-k8s-configs
```

## Global Options

These options apply to the `kustomtrace` command itself and can be used with any subcommand:

* `-a=<appsDir>`, `--apps-dir=<appsDir>`: **(Required)** Path to the root directory containing your Kustomize applications. This directory is the main scope for analysis.
* `--log-file=<logFile>`: (Optional) Specify a file to write detailed logs (INFO, WARN, ERROR, including stack traces) to. If this option is used, console output from logging will be minimized.
* `--log-level=<LEVEL>`: (Optional) Set the verbosity of logs. Affects both console (if `--log-file` is not used) and the log file (if specified). Levels: `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`. Default for console is typically `WARN`; for file is `INFO`.
* `-h`, `--help`: Display help information for `kustomtrace` or a specific subcommand.
* `-V`, `--version`: Display version information for `kustomtrace`.

**Note on Path Output:**
All file paths in the CLI's output (especially in the YAML format via the `--output` option, but also generally in console output) use **forward slashes (`/`)** as path separators. This ensures consistent and predictable output across all operating systems.

## Available Subcommands

Below are the available subcommands. For detailed help on a specific subcommand, run:
`java -jar kustomtrace-cli.jar <subcommand> --help`

---
### `list-root-apps`

**Description:** Lists all root Kustomize applications found within the specified `--apps-dir`. A root app is a directory containing a kustomization file that is not referenced by any other app in the scanned structure.

**Usage:**
`kustomtrace list-root-apps [-o=<outputFile.yaml>]`
*(Requires global `-a, --apps-dir`)*

**Options for `list-root-apps`:**
* `-o=<outputFile.yaml>`, `--output=<outputFile.yaml>`: (Optional) Output the list of root application paths to the specified YAML file.

**Example Invocation:**
```bash
java -jar kustomtrace-cli.jar -a ./my-k8s-configs list-root-apps -o roots.yaml
```

**Console Output (if `-o` is not used):**
```
Root Applications:
- path/to/app1-dir
- another/app/dir
  ```
  If no root apps are found:
  ```
  No root applications found in: <appsDir_path>
  ```

**YAML Output (e.g., `-o roots.yaml`):**
```yaml
root-apps:
- path/to/app1-dir # Paths are relative to --apps-dir
- another/app/dir
  ```

---
### `app-files`

**Description:** Lists all unique files used by a given application. This includes its own kustomization file and all transitively resolved resources, bases, and components.

**Usage:**
`kustomtrace app-files <appPath> [-o=<outputFile.yaml>]`
*(Requires global `-a, --apps-dir`)*

**Arguments for `app-files`:**
* `<appPath>`: **(Required)** Path to the application directory (containing a kustomization file) . This path is typically relative to the global `--apps-dir` or can be an absolute path.

**Options for `app-files`:**
* `-o=<outputFile.yaml>`, `--output=<outputFile.yaml>`: (Optional) Output the list of files to the specified YAML file.

**Example Invocation:**
```bash
java -jar kustomtrace-cli.jar -a ./all-apps app-files path/to/my-app -o my-app-files.yaml
```

**Console Output:**
```
Files used by application 'my-app':
- configmap.yaml
- deployment.yaml
- kustomization.yaml
- ../base/service.yaml
  ```

**YAML Output (e.g., `-o my-app-files.yaml`):**
```yaml
app-files:
my-app: # Key is the app's directory identifier (relative to --apps-dir)
- configmap.yaml # Paths are relative to the app's own directory
- deployment.yaml
- kustomization.yaml
- ../base/service.yaml
```

---
### `affected-apps`

**Description:** Finds and lists Kustomize applications that are (directly or indirectly) affected by changes in one or more specified files.

**Usage:**
`kustomtrace affected-apps [modifiedFile...] [-f=<filesFromFile>] [-o=<outputFile.yaml>]`
*(Requires global `-a, --apps-dir`)*

**Arguments for `affected-apps`:**
* `[modifiedFile...]`: (Optional) Space-separated paths to one or more modified files. These paths can be absolute or relative to the current working directory.

**Options for `affected-apps`:**
* `-f=<filesFromFile>`, `--files-from-file=<filesFromFile>`: (Optional) Path to a text file containing a list of modified file paths (one path per line).
* `-o=<outputFile.yaml>`, `--output=<outputFile.yaml>`: (Optional) Output the list of affected applications to the specified YAML file.

**Example Invocation:**
```bash
java -jar kustomtrace-cli.jar -a ./all-apps affected-apps common/base.yaml services/service-patch.yaml -o impact.yaml
```
```bash
java -jar kustomtrace-cli.jar -a ./all-apps affected-apps -f changed_files.txt -o impact.yaml
```

**Console Output:**
```
Affected Applications:
Affected apps by common/base.yaml:
- app1/production
- app2/staging
  Affected apps by services/service-patch.yaml:
- app1/production
  Affected apps by path/to/unreferenced-file.yaml:
  Warning: File ... is not referenced by any app
  Summary: No applications were found to be affected by the specified file(s). ```

**YAML Output (e.g., `-o impact.yaml`):**
```yaml
affected-apps:
common/base.yaml: # Key is the modified file path (as provided by user, normalized)
- app1/production # Affected app paths are relative to --apps-dir
- app2/staging
path/to/unreferenced-or-external-file.yaml: [] # If a modified file is unreferenced or affects no apps
```

## Use Cases & Examples

* **CI/CD - Selective Builds/Tests:**
  In a CI pipeline, when a pull request changes certain files, use `affected-apps` to determine which application folders are impacted. Then, only build, test, or deploy those specific applications.
  ```bash
  # Assuming git diff --name-only origin/main...HEAD > changed_files.txt
  java -jar kustomtrace-cli.jar affected-apps \
  -a /workspace/kubernetes-config \
  -f changed_files.txt \
  -o impacted_apps.yaml

  # Then parse impacted_apps.yaml to trigger downstream jobs
  ```

* **Repository Inspection & Impact Analysis.**

## Current Limitations
* **Supported Kustomize Fields:** The CLI currently analyzes references from `resources`, `bases`, `components`, `patches` (`path` key), `patchesStrategicMerge`, and `configMapGenerator` (`envs`, `files`). Support for more fields may be added.

## Future Development Ideas
* **User-Selectable App Path Representation:** An option like `--represent-app-as <folder|file>` to control how application paths are displayed in the output.
* **Bulk Queries:** Combine multiple queries into a single execution and output file.
* **Chained Queries:** Use the output of one command as input for another, leveraging the pre-built graph for efficiency.