# KustomTrace CLI

The KustomTrace Command-Line Interface (CLI) allows you to analyze Kustomize directory structures and dependencies directly from your terminal or in scripts.

## Installation

*(Provide instructions on how to obtain or build the CLI executable. Examples:)*
* *Download pre-compiled binaries from the [Releases page](link-to-releases).*
* *Build from source: `./gradlew :kustomtrace-cli:shadowJar` (or equivalent) and find the executable JAR in `kustomtrace-cli/build/libs/`.)*

**Basic Usage:**
`java -jar kustomtrace-cli.jar <command> [options]`
*(Or, if you have a wrapper script: `kustomtrace <command> [options]`)*

## Global Options

These options are applicable to most commands:

* `-a=<appsDir>`, `--apps-dir=<appsDir>`: **(Required)** Path to the root directory containing your Kustomize applications.
* `--log-file=<logFile>`: (Optional) Specify a file to write detailed logs (INFO, WARN, ERROR) to. Errors and warnings from command logic will also be directed here if specified.
* `-h`, `--help`: Display help information.
* `-V`, `--version`: Display version information.

**Note on Path Output:**
All file paths in the CLI's output (especially in the YAML format via the `--output` option, but also generally in console output) use **forward slashes (`/`)** as path separators. This ensures consistent and predictable output across all operating systems (Windows, Linux, macOS).

## Available Commands

### `list-root-apps`
Lists all root Kustomize applications found in the specified directory. A root app is a kustomization that is not referenced by any other kustomization.

**Usage:**
`kustomtrace-cli list-root-apps -a=<appsDir> [-o=<outputFile.yaml>]`

**Example Invocation:**
`java -jar kustomtrace-cli.jar list-root-apps -a ./my-k8s-configs -o roots.yaml`

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

**YAML Output (`-o roots.yaml`):**
```yaml
root-apps:
- path/to/app1-dir # Paths are relative to --apps-dir
- another/app/dir
```

### `app-files`
Lists all unique files used by a given application (including its kustomization file and all resolved resources and bases).

**Usage:**
`kustomtrace-cli app-files -a=<appsDir> <appPath> [-o=<outputFile.yaml>]`

* `<appPath>`: Path to the application directory or its specific kustomization file (e.g., `path/to/my-app` or `path/to/my-app/kustomization.yaml`).

**Example Invocation:**
`java -jar kustomtrace-cli.jar app-files -a ./all-apps path/to/my-app -o my-app-files.yaml`

**Console Output:**
```
Files used by application 'my-app':
- configmap.yaml
- deployment.yaml
- kustomization.yaml
- ../base/service.yaml
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

### `affected-apps`
Finds applications affected by changes in one or more specified files.

**Usage:**
`kustomtrace-cli affected-apps -a=<appsDir> [modifiedFile...] [-f=<filesFromFile>] [-o=<outputFile.yaml>]`

* `[modifiedFile...]`: (Optional) Space-separated paths to modified files.
* `-f=<filesFromFile>`: (Optional) Path to a file containing a list of modified file paths (one path per line).

**Example Invocation:**
`java -jar kustomtrace-cli.jar affected-apps -a ./all-apps common/base.yaml services/service-patch.yaml -o impact.yaml`

**Console Output:**
```
Affected Applications:
Affected apps by common/base.yaml:
- app1/production
- app2/staging
  Affected apps by services/service-patch.yaml:
- app1/production
  Summary: No applications were found to be affected by the specified file(s). ```
```
**YAML Output (`-o impact.yaml`):**
```yaml
affected-apps:
common/base.yaml: # Key is the modified file path (as provided, or relative to appsDir if within)
- app1/production # Affected app paths are relative to appsDir
- app2/staging
unreferenced-or-external-file.yaml: [] # If a file is unreferenced or affects no apps
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