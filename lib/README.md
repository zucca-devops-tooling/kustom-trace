[![Javadoc](https://img.shields.io/badge/Javadoc-Available-brightgreen)](https://zucca-devops-tooling.github.io/kustom-trace/javadoc)
# KustomTrace Library

The library builds a dependency graph for a local Kustomize repository and exposes simple queries for root apps, affected apps, and application files.

If you only need command-line usage, the project also ships a CLI, including native binaries for Linux, Windows, and macOS x64. See [../cli/README.md](../cli/README.md).

## Add the Dependency

Maven:

```xml
<dependency>
  <groupId>dev.zucca-ops</groupId>
  <artifactId>kustomtrace</artifactId>
  <version>1.0.1</version>
</dependency>
```

Gradle:

```gradle
implementation("dev.zucca-ops:kustomtrace:1.0.1")
```

## Quick Start

```java
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

Path appsDir = Path.of("apps");

try {
    KustomTrace trace = KustomTrace.fromDirectory(appsDir);

    List<Path> rootApps = trace.getRootApps();
    List<Path> affectedApps = trace.getAppsWith(appsDir.resolve("base/common.yaml"));
    List<Path> appFiles = trace.getDependenciesFor(appsDir.resolve("payments/prod"));

    System.out.println(rootApps);
    System.out.println(affectedApps);
    System.out.println(appFiles);
} catch (IOException | KustomException e) {
    e.printStackTrace();
}
```

## Main API

- `KustomTrace.fromDirectory(Path appsDir)`: scans the repository and builds the graph
- `getRootApps()`: returns root application directories
- `getAppsWith(Path file)`: returns root application directories that depend on a file
- `getDependenciesFor(Path appDir)`: returns every file used by an application
- `getGraph()`: gives direct access to the underlying graph model

The facade methods return `Path` objects, not display-ready strings. In practice these paths are absolute, normalized filesystem paths from the built graph. If you need portable text output, normalize it in your own code.

## Graph Model

The public model types are:

- `KustomGraph`: the full dependency graph
- `Kustomization`: a parsed Kustomization file
- `KustomFile`: a referenced YAML, YML, or JSON resource file
- `KustomResource`: a parsed Kubernetes resource inside a `KustomFile`
- `ResourceReference`: a typed edge from a `Kustomization` to another graph node

Example of dropping to the graph:

```java
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import dev.zucca_ops.kustomtrace.model.ResourceReference;
import java.nio.file.Path;

KustomGraph graph = trace.getGraph();
Kustomization app = graph.getKustomization(
        Path.of("apps/payments/prod/kustomization.yaml").toAbsolutePath().normalize());

if (app != null) {
    for (ResourceReference reference : app.getReferences()) {
        System.out.println(reference.type() + " -> " + reference.resource().getPath());
    }
}
```

## Supported References

KustomTrace currently traverses these Kustomize reference types:

- `resources`
- `bases`
- `components`
- `patches[].path`
- `patchesStrategicMerge`
- `configMapGenerator.envs`
- `configMapGenerator.files`
- `secretGenerator.envs`
- `secretGenerator.files`

Recognized file names:

- Kustomization files: `kustomization.yaml`, `kustomization.yml`, `Kustomization`
- Resource files: `.yaml`, `.yml`, `.json`

## Notes

- `getDependenciesFor(...)` includes the application's own kustomization file.
- Circular dependencies are handled during traversal and logged.
- Files that are not part of the graph cause `UnreferencedFileException`.

For full API details, use the [Javadoc](https://zucca-devops-tooling.github.io/kustom-trace/javadoc).
