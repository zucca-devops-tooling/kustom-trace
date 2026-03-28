# KustomTrace

KustomTrace analyzes a local Kustomize repository, builds a dependency graph, and answers three practical questions:

- Which directories are root applications?
- Which files does an application depend on?
- Which applications are affected by a changed file?

It is available as a Java library and as a CLI.

## Modules

- [CLI documentation](./cli/README.md)
- [Library documentation](./lib/README.md)
- [Javadoc](https://zucca-devops-tooling.github.io/kustom-trace/javadoc)

## What KustomTrace Understands

KustomTrace scans local Kustomize trees and recognizes:

- Kustomization files: `kustomization.yaml`, `kustomization.yml`, `Kustomization`
- Resource files: `.yaml`, `.yml`, `.json`
- Reference fields: `resources`, `bases`, `components`, `patches[].path`, `patchesStrategicMerge`, `configMapGenerator.{envs,files}`, `secretGenerator.{envs,files}`

The focus is local filesystem analysis. If a reference is not represented by one of the fields above, it is not included in the graph.

## CLI Quick Start

The CLI is distributed as:

- A shaded JAR: `kustomtrace-cli-<version>-all.jar`
- Linux native archive: `kustomtrace-linux-x64.tar.gz`
- Windows native archive: `kustomtrace-windows-x64.zip`
- macOS native archive: `kustomtrace-macos-x64.tar.gz`

The native binaries run without a local Java installation. Older releases may only include the JAR, so check the assets on the release you want to use.

Example with a native binary:

```bash
./kustomtrace-linux-x64 --apps-dir ./apps list-root-apps
./kustomtrace-linux-x64 --apps-dir ./apps affected-apps ./apps/base/common.yaml
```

Example with the shaded JAR:

```bash
java -jar kustomtrace-cli-1.1.0-all.jar --apps-dir ./apps list-root-apps
java -jar kustomtrace-cli-1.1.0-all.jar --apps-dir ./apps affected-apps ./apps/base/common.yaml
```

See [cli/README.md](./cli/README.md) for installation details, command examples, YAML output, logging, and native build instructions.

## Library Quick Start

Maven:

```xml
<dependency>
  <groupId>dev.zucca-ops</groupId>
  <artifactId>kustomtrace</artifactId>
  <version>1.1.0</version>
</dependency>
```

Gradle:

```gradle
implementation("dev.zucca-ops:kustomtrace:1.1.0")
```

Basic usage:

```java
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import java.io.IOException;
import java.nio.file.Path;

Path appsDir = Path.of("apps");

try {
    KustomTrace trace = KustomTrace.fromDirectory(appsDir);

    System.out.println(trace.getRootApps());
    System.out.println(trace.getAppsWith(appsDir.resolve("base/common.yaml")));
    System.out.println(trace.getDependenciesFor(appsDir.resolve("payments/prod")));
} catch (IOException | KustomException e) {
    e.printStackTrace();
}
```

See [lib/README.md](./lib/README.md) for the API surface and graph model.

## Native Binaries

The native CLI packages are built with GraalVM Native Image and published for:

- Linux x64
- Windows x64
- macOS x64

Release assets also include SHA-256 checksum files next to each archive.

If you build locally, the relevant tasks are:

```bash
./gradlew :kustomtrace-cli:shadowJar
./gradlew :kustomtrace-cli:nativeCompile
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Typical Uses

- CI and pull request automation: detect only the applications touched by a change
- Repository audits: discover entry points and transitive dependencies
- Safer refactors: understand blast radius before changing shared bases or generator inputs

There is also a working demo repository here:

- [Kustomize and Kyverno at Scale: A CI/CD Demonstration](https://github.com/zucca-devops-tooling/kustomize-at-scale-demo)

## Contributing

- Issues and ideas: [open an issue](https://github.com/zucca-devops-tooling/kustom-trace/issues/new)
- Contributions: see [CONTRIBUTING.md](./CONTRIBUTING.md)

## License

Apache License 2.0. See [LICENSE](./LICENSE).
