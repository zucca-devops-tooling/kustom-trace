# KustomTrace CLI

The KustomTrace Command-Line Interface (CLI) allows you to analyze Kustomize directory structures and dependencies directly from your terminal or in scripts.

## Installation

1.  Go to the **[KustomTrace GitHub Releases page](https://github.com/zucca-devops-tooling/kustom-trace/releases)**.
2.  Select the desired release version (e.g., the latest stable release).
3.  From the "Assets" section of that release, download the `kustomtrace-cli-VERSION-all.jar` file.

**Requirements:**
* Java 17 (or newer) is required to run the JAR.

*(Native binaries for direct execution without needing a Java installation are planned for future releases.)*

## Basic Invocation

The general way to run the CLI is:

```bash
java -jar kustomtrace-cli-VERSION.jar [GLOBAL_OPTIONS] <subcommand> [SUBCOMMAND_OPTIONS_ARGS]
```

**Important Note on Option Order:** Global options like `--apps-dir` or `--output` must be placed **before** the subcommand name (e.g., `list-root-apps`).

## Global Options

These options apply to the `kustomtrace` command itself and must be used before a subcommand:

* `-a=<appsDir>`, `--apps-dir=<appsDir>`: **(Required)** Path to the root directory containing your Kustomize applications.
* `-o=<outputFile.yaml>`, `--output=<outputFile.yaml>`: (Optional) Output the command's results to the specified YAML file.
* `--log-file=<logFile>`: (Optional) Specify a file to write detailed logs to. If used, console logging will be minimized.
* `--log-level=<LEVEL>`: (Optional) Set the verbosity of logs. Levels: `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`.
* `-h`, `--help`: Display help information.
* `-V`, `--version`: Display version information.

**Note on Path Output:**
All file paths in the CLI's output use **forward slashes (`/`)** as path separators for cross-platform consistency.

## Available Subcommands

For detailed help on any subcommand, run `java -jar kustomtrace-cli.jar <subcommand> --help`.

---
### `list-root-apps`

**Description:**
Lists all "root applications" found. A root application is identified by a `kustomization.yaml` (or `.yml`/`Kustomization`) file that is not referenced by any other kustomization in the scanned structure.

**Usage:**
`kustomtrace -a=<appsDir> [-o=<outputFile.yaml>] list-root-apps`

**Example Invocation:**
```bash
java -jar kustomtrace-cli.jar -a ./my-k8s-configs -o roots.yaml list-root-apps
```

**YAML Output (`-o roots.yaml`):**
```yaml
root-apps:
- path/to/app1-dir # Paths are relative to --apps-dir
- another/app/dir
  ```

---
### `app-files`

**Description:**
Lists all unique files used by a given application, including its own kustomization file and all transitively resolved resources and bases.

**Usage:**
`kustomtrace -a=<appsDir> [-o=<outputFile.yaml>] app-files <appPath>`

**Arguments for `app-files`:**
* `<appPath>`: **(Required)** Path to the application directory or its specific kustomization file.

**Example Invocation:**
```bash
java -jar kustomtrace-cli.jar -a ./all-apps -o my-app-files.yaml app-files path/to/my-app
```

**YAML Output (`-o my-app-files.yaml`):**
```yaml
app-files:
my-app: # Key is the app's directory, relative to --apps-dir
- configmap.yaml # Paths are relative to the app's own directory
- deployment.yaml
- kustomization.yaml
- ../base/service.yaml
```

---
### `affected-apps`

**Description:**
Finds and lists Kustomize applications that are (directly or indirectly) affected by changes in one or more specified files.

**Usage:**
`kustomtrace -a=<appsDir> [-o=<outputFile.yaml>] affected-apps [modifiedFile...] [-f=<filesFromFile>]`

**Arguments & Options for `affected-apps`:**
* `[modifiedFile...]`: (Optional) Space-separated paths to one or more modified files.
* `-f=<filesFromFile>`, `--files-from-file=<filesFromFile>`: (Optional) Path to a text file containing a list of modified file paths (one path per line).

**Example Invocation:**
```bash
java -jar kustomtrace-cli.jar -a ./all-apps -o impact.yaml affected-apps common/base.yaml services/service-patch.yaml
```

**YAML Output (`-o impact.yaml`):**
```yaml
affected-apps:
common/base.yaml: # Key is the modified file path
- app1/production
- app2/staging
unreferenced-or-external-file.yaml: [] # Ignored files with no affected apps will not appear
```

## Use Cases & Examples

* **CI/CD - Selective Builds/Tests:**
  In a CI pipeline, when a pull request changes certain files, use `affected-apps` to determine which application folders are impacted. Then, only build, test, or deploy those specific applications.
  ```bash
  # Assuming git diff --name-only origin/main...HEAD > changed_files.txt
  java -jar kustomtrace-cli.jar -a /workspace/kubernetes-config -o impacted_apps.yaml affected-apps -f changed_files.txt

  # Then parse impacted_apps.yaml to trigger downstream jobs
  ```

---
*(Limitations and Future Work sections can remain as previously discussed)*