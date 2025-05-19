package model;

import exceptions.NotAnAppException;
import exceptions.UnreferencedFileException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import parser.ReferenceType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class KustomGraphTest {
    @TempDir
    Path tempDir;

    @Test
    void resolvesEquivalentPathsToSameNode() {
        Path original = tempDir.resolve("app/kustomization.yaml").normalize();
        Path variant1 = original.toAbsolutePath();
        Path variant2 = tempDir.resolve("./app/kustomization.yaml").toAbsolutePath().normalize();

        Kustomization k = new Kustomization(original, Map.of());
        KustomGraph graph = new KustomGraph();

        graph.addNode(k);

        assertTrue(graph.containsNode(variant1));
        assertTrue(graph.containsNode(variant2));

        assertSame(k, graph.getNode(variant1));
        assertSame(k, graph.getNode(variant2));
    }

    @Test
    void getApps_returnsOnlyRootKustomizations() {
        Kustomization root = new Kustomization(tempDir.resolve("root/kustomization.yaml"), Map.of());
        Kustomization child = new Kustomization(tempDir.resolve("child/kustomization.yaml"), Map.of());

        // Manually wire up dependency (child is not root)
        child.addDependent(root);

        KustomGraph graph = new KustomGraph();
        graph.addNode(root);
        graph.addNode(child);

        List<Kustomization> apps = graph.getApps();

        assertEquals(1, apps.size());
        assertSame(root, apps.get(0));
    }

    @Test
    void getAppsWith_returnsAppList_forReferencedFile() throws Exception {
        Kustomization app = new Kustomization(tempDir.resolve("app/kustomization.yaml"), Map.of());
        app.dependents = List.of(); // root

        KustomGraph graph = new KustomGraph();
        graph.addNode(app);

        List<Kustomization> result = graph.getAppsWith(app.getPath());

        assertEquals(1, result.size());
        assertSame(app, result.get(0));
    }

    @Test
    void getAppsWith_throwsForUnreferencedFile() {
        KustomGraph graph = new KustomGraph();
        Path path = tempDir.resolve("not-tracked.yaml");

        assertThrows(UnreferencedFileException.class, () -> graph.getAppsWith(path));
    }

    @Test
    void getAllAppFiles_returnsAllInvolvedPaths() throws Exception {
        Path rootPath = tempDir.resolve("root/kustomization.yaml");
        Path dep1 = tempDir.resolve("base/a.yaml");
        Path dep2 = tempDir.resolve("base/b.yaml");

        // Set up root app
        Kustomization root = new Kustomization(rootPath, Map.of());

        // Add references (each with their own paths)
        root.addReference(new ResourceReference(ReferenceType.RESOURCE, new DummyNode(dep1)));
        root.addReference(new ResourceReference(ReferenceType.RESOURCE, new DummyNode(dep2)));

        KustomGraph graph = new KustomGraph();
        graph.addNode(root);

        // Invoke
        List<Path> result = graph.getAllAppFiles(rootPath);

        // Expect all 3 paths: dep1, dep2, and rootPath
        assertEquals(3, result.size());
        assertTrue(result.containsAll(List.of(dep1, dep2, rootPath)));
    }

    @Test
    void getAppDependencies_throwsIfNotRoot() {
        Path path = tempDir.resolve("child/kustomization.yaml");
        Kustomization child = new Kustomization(path, Map.of());

        // Mark as non-root
        child.addDependent(new Kustomization(tempDir.resolve("other/kustomization.yaml"), Map.of()));

        KustomGraph graph = new KustomGraph();
        graph.addNode(child);

        assertThrows(NotAnAppException.class, () -> graph.getAllAppFiles(path));
    }

    // Minimal stub node for testing references
    static class DummyNode extends GraphNode {
        DummyNode(Path path) {
            this.path = path;
        }

        @Override public Stream<Kustomization> getApps() { return Stream.empty(); }
        @Override public Boolean isRoot() { return true; }
        @Override public Stream<Path> getDependencies() { return Stream.of(path); }

        @Override
        Stream<Path> getDependencies(Set<GraphNode> visited) {
            return Stream.of(path);
        }

        @Override
        public boolean isKustomization() {
            return false;
        }

        @Override
        public boolean isKustomFile() {
            return true;
        }
    }
}
