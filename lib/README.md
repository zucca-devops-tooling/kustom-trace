[![Javadoc](https://img.shields.io/badge/Javadoc-Available-brightgreen)](https://zucca-devops-tooling.github.io/kustom-trace/javadoc)
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

// ...
Path specificAppPath = Paths.get("my-app-overlay"); // Path to an app dir or its kustomization file
List<Path> appFiles = kustomTrace.getDependenciesFor(specificAppPath);
System.out.println("Files for app " + specificAppPath + ":");
appFiles.forEach(filePath -> System.out.println("  - " + filePath));
```

**3. Finding Applications Affected by a File Change:**
```java
// ...
Path changedFile = Paths.get("common/base/configmap.yaml");
List<Path> affectedApps = kustomTrace.getAppsWith(changedFile);
System.out.println("Applications affected by changes in " + changedFile + ":");
affectedApps.forEach(appPath -> System.out.println("  - " + appPath));
```

## Advanced Usage: Navigating the Graph

Once you have a `KustomTrace` instance, you can retrieve the `KustomGraph` object for more detailed analysis and direct graph traversal. This is useful for scenarios beyond the direct facade methods.


First, get a reference to the graph and a specific Kustomization node:

```java
KustomGraph graph = kustomTrace.getGraph();
Path myAppKustomizationPath = Paths.get("my-app/staging/kustomization.yaml"); // Path relative to your --apps-dir
Kustomization myApp = graph.getKustomization(myAppKustomizationPath);

if (myApp == null) {
    System.err.println("Application kustomization not found in graph: " + myAppKustomizationPath);
    return; // Or handle error appropriately
}
```

Now you can explore its relationships:

**1. Get Direct Dependencies (as GraphNode objects)**

A Kustomization directly references other Kustomizations (bases, components) or KustomFiles (resources, patches). You can access these referenced nodes:

```java
System.out.println("Directly referenced nodes by " + myApp.getDisplayName() + ":");
myApp.getDirectlyReferencedNodes() // Or myApp.getReferences().stream().map(ResourceReference::resource)
    .forEach(node -> {
        System.out.println("- " + node.getDisplayName() +
        " (Type: " + (node.isKustomization() ? "Kustomization" : "KustomFile") + ")");
    });
```

**2. Get Direct Dependents (Kustomizations that reference this node)**

You can find out which Kustomization files directly depend on (i.e., reference) the current `myApp` node:

```java
System.out.println("\n'" + myApp.getDisplayName() + "' is directly depended upon by Kustomizations:");
myApp.getDependents().forEach(dependent -> {
    System.out.println("- " + dependent.getDisplayName());
});
```

**3. Inspecting KustomFiles and their KustomResources (Handling Multi-Document YAML)**

If a `GraphNode` is a `KustomFile`, it may contain one or more Kubernetes resources (especially if the source YAML file uses `---` separators for multiple documents). `GraphNodeResolver` (via `YamlParser.parseFile`) handles this by creating multiple `KustomResource` objects for a single `KustomFile` if applicable. You can access these as follows:

```java
// Assuming 'dependencyNode' is a GraphNode obtained from myApp.getDirectlyReferencedNodes()
if (dependencyNode.isKustomFile()) {
    KustomFile kustomFile = (KustomFile) dependencyNode;
    System.out.println("\nResources in KustomFile: " + kustomFile.getDisplayName());
    kustomFile.getResources().forEach(resource -> {
    // KustomResource contains 'kind' and 'name' parsed from the YAML document
    System.out.println("  - Kind: " + resource.getKind() + ", Name: " + resource.getName());
    });

    // kustomFile.getResource() provides a shortcut to the first resource or a default.
}
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
    <artifactId>kustomtrace</artifactId> <version>1.0.0</version>
</dependency>
```


**(Example - Gradle)**
```gradle
implementation 'dev.zucca_ops:kustomtrace-lib:1.0.0
```
