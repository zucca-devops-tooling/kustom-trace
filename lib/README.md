# KustomTrace Library

The KustomTrace Library provides a powerful Java API to programmatically parse Kustomize configurations, build a dependency graph, and query relationships between your applications and resources.

## Core Concepts

The library revolves around a few key concepts:

* **`KustomGraph`**: The central data structure representing the directed graph of all discovered Kustomize files (`Kustomization` nodes) and resource files (`KustomFile` nodes). Paths within the graph are typically stored relative to the initial `appsDir`.
* **`GraphNode`**: An abstract representation of a node in the graph, with concrete implementations:
    * **`Kustomization`**: Represents a `kustomization.yaml` (or `.yml`/`Kustomization`) file and its parsed content, including references to other resources, bases, and components.
    * **`KustomFile`**: Represents a standard Kubernetes manifest file (YAML/JSON) containing one or more `KustomResource` definitions.
* **`KustomResource`**: Represents a single Kubernetes resource (e.g., Deployment, Service) parsed from a `KustomFile`.
* **`ResourceReference`**: Represents a typed link from a `Kustomization` to another `GraphNode` (e.g., a "resource", "base", "component" reference).
* **"Root App"**: A `Kustomization` that is not referenced as a resource or component by any other `Kustomization` file within the scanned directory structure. These are typically the entry points for your applications or overlays.
    * **Common Scenarios for Root Apps:**
        1.  **Orphaned Kustomizations:** Previously referenced kustomizations whose links have been removed (e.g., due to code cleanup, refactoring, or features being deprecated) but the files themselves were not deleted (perhaps due to oversight, or kept "just in case" they are later needed).
        2.  **Intentionally Independent Overlays:** Kustomizations that are designed to be standalone entry points, even if they are located deep within the file hierarchy, and are not intended to be included as bases or components by other kustomizations.

## Main Facade: `KustomTrace`

The primary entry point for using the library is the `dev.zucca_ops.kustomtrace.KustomTrace` class.

### Initialization

First, build the Kustomize graph from a specified applications directory:

```java
import dev.zucca_ops.kustomtrace.KustomTrace;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

// ...

try {
    Path appsRootDir = Paths.get("/path/to/your/kubernetes/apps");
    KustomTrace kustomTrace = KustomTrace.fromDirectory(appsRootDir);
    // The kustomTrace instance now holds the fully built graph.
} catch (IOException e) {
    // Handle exceptions during directory scanning or initial parsing
    e.printStackTrace();
}
```

### Key Methods

Once you have a `KustomTrace` instance:

* **`List<Path> getRootApps()` (or `getRootAppPaths()`):** Returns a list of paths representing the root applications found in the graph.
    * *(Future Work Note: This method's output path representation—directory vs. kustomization file—could become configurable).*
* **`List<Path> getAppsWith(Path modifiedFile)`**: Given a path to a file, returns a list of paths for applications that directly or indirectly reference this file.
* **`List<Path> getDependenciesFor(Path appPathInput)`**: Given a path to an application (either its directory or its kustomization file), returns a list of all unique file paths that this application depends on. Paths are relative to the application's own directory.
* **`KustomGraph getGraph()`**: Provides access to the underlying `KustomGraph` object for more advanced queries.

### Path Representation
All paths returned by `KustomTrace` facade methods intended for display or as identifiers are normalized to use **forward slashes (`/`)** as separators for cross-platform consistency.

## Usage Examples

**1. Listing Root Applications:**
```java
// Assuming kustomTrace is an initialized KustomTrace instance
List<Path> rootApps = kustomTrace.getRootApps();
System.out.println("Root Applications:");
rootApps.forEach(appPath -> System.out.println("- " + appPath));
```

**2. Getting All Files for a Specific Application:**
```java
import dev.zucca_ops.kustomtrace.exceptions.KustomException; // Or specific sub-exceptions

// ...
try {
    Path specificAppPath = Paths.get("my-app-overlay"); // Path to an app dir or its kustomization file
    List<Path> appFiles = kustomTrace.getDependenciesFor(specificAppPath);
    System.out.println("Files for app " + specificAppPath + ":");
    appFiles.forEach(filePath -> System.out.println("  - " + filePath));
} catch (KustomException e) {
    System.err.println("Error getting dependencies: " + e.getMessage());
}
```

**3. Finding Applications Affected by a File Change:**
```java
import dev.zucca_ops.kustomtrace.exceptions.UnreferencedFileException; // Or KustomException

// ...
try {
    Path changedFile = Paths.get("common/base/configmap.yaml");
    List<Path> affectedApps = kustomTrace.getAppsWith(changedFile);
    System.out.println("Applications affected by changes in " + changedFile + ":");
    affectedApps.forEach(appPath -> System.out.println("  - " + appPath));
} catch (UnreferencedFileException e) {
    System.err.println("The file " + e.getPath() + " is not referenced by any application in the graph.");
}
```

## Advanced Usage

For more complex scenarios, retrieve the `KustomGraph` object:
```java
KustomGraph graph = kustomTrace.getGraph();
// Example: Iterate all nodes
// graph.getNodeMap().forEach((path, node) -> { /* ... */ });
```

## Use Cases

* **Build Automation:** Programmatically determine which applications need rebuilding.
* **Dependency Visualization Tools.**
* **Custom Linting/Policy Enforcement.**
* **Inventory Management.**

## Adding as a Dependency

**(Example - Maven)**
```xml
<dependency>
    <groupId>dev.zucca_ops</groupId>
    <artifactId>kustomtrace-lib</artifactId> <version>YOUR_PROJECT_VERSION</version>
</dependency>
```


**(Example - Gradle)**
```gradle
implementation 'dev.zucca_ops:kustomtrace-lib:YOUR_PROJECT_VERSION' // Or your actual coordinates
```


*(Note: Javadoc generated from your documented source code using `gradle javadoc` or `mvn javadoc:javadoc` would be the primary way to provide detailed API documentation. This README gives an overview and usage patterns.)*