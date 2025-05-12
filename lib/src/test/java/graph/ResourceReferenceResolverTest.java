package graph;

import exceptions.InvalidReferenceException;
import model.GraphNode;
import model.KustomFile;
import model.Kustomization;
import model.ResourceReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import parser.ReferenceType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ResourceReferenceResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveDependency_wrapsResolveDirectory_forDirectory() {
        // Directory layout:
        // tempDir/
        //   kustomization.yaml  (the app)
        //   component1/          (dependency)
        //   component2/

        // Prepare fake kustomization.yaml content
        Map<String, Object> yaml = Map.of(
                "components", List.of("component1", "component2"),
                "namePrefix", "dev-"  // this should be ignored
        );

        Path kustomPath = tempDir.resolve("kustomization.yaml");
        Path dep1 = tempDir.resolve("component1");
        Path dep2 = tempDir.resolve("component2");

        Kustomization root = new Kustomization(kustomPath, yaml);
        Kustomization c1 = new Kustomization(dep1.resolve("kustomization.yaml"), Map.of());
        Kustomization c2 = new Kustomization(dep2.resolve("kustomization.yaml"), Map.of());

        ResourceReference ref1 = new ResourceReference(ReferenceType.COMPONENT, c1);
        ResourceReference ref2 = new ResourceReference(ReferenceType.COMPONENT, c2);

        // Spy resolver to fake resolveDependency() responses
        ResourceReferenceResolver resolver = spy(new ResourceReferenceResolver(null));
        doReturn(Stream.of(ref1)).when(resolver).resolveDependency(ReferenceType.COMPONENT, dep1);
        doReturn(Stream.of(ref2)).when(resolver).resolveDependency(ReferenceType.COMPONENT, dep2);

        // Run
        Stream<ResourceReference> stream = resolver.resolveDependencies(root);
        List<ResourceReference> results = stream.toList();

        // Verify
        assertEquals(2, results.size());
        assertTrue(results.contains(ref1));
        assertTrue(results.contains(ref2));
    }

    @Test
    void resolveDependency_returnsEmptyStream_whenValidationFails() throws InvalidReferenceException {
        ReferenceType type = spy(ReferenceType.PATCH); // any type works

        // force validate() to throw
        doThrow(new InvalidReferenceException("Expected a file", tempDir)).when(type).validate(tempDir);

        ResourceReferenceResolver resolver = new ResourceReferenceResolver(null);
        Stream<ResourceReference> result = resolver.resolveDependency(type, tempDir);

        assertTrue(result.toList().isEmpty());
    }

    @Test
    void resolveDependency_wrapsBuildKustomFile_forFile() throws IOException {
        Path filePath = tempDir.resolve("config.yaml");
        Files.writeString(filePath, "kind: ConfigMap");

        KustomFile file = new KustomFile(filePath);

        // mock builder
        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        when(builder.buildKustomFile(filePath)).thenReturn(file);

        ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);

        Stream<ResourceReference> result = resolver.resolveDependency(ReferenceType.RESOURCE, filePath);
        ResourceReference ref = result.findFirst().orElseThrow();

        assertEquals(filePath, ref.resource().getPath());
        assertEquals(ReferenceType.RESOURCE, ref.referenceType());
    }

    @Test
    void resolveDependencies_resolvesMultipleComponentReferences() throws IOException {
        Path dirPath = tempDir.resolve("base");
        Files.createDirectory(dirPath);
        Files.createFile(dirPath.resolve("kustomization.yaml"));

        Kustomization node = new Kustomization(dirPath.resolve("kustomization.yaml"), Map.of());

        ResourceReferenceResolver resolver = spy(new ResourceReferenceResolver(null));
        doReturn(Stream.of(node)).when(resolver).resolveDirectory(dirPath);

        Stream<ResourceReference> result = resolver.resolveDependency(ReferenceType.COMPONENT, dirPath);
        ResourceReference ref = result.findFirst().orElseThrow();

        assertEquals(node, ref.resource());
        assertEquals(ReferenceType.COMPONENT, ref.referenceType());
    }


    @Test
    void resolveDirectory_returnsKustomization_ifPresent() throws IOException {
        Path dir = tempDir.resolve("with-kustomization");
        Files.createDirectory(dir);
        Path kustomPath = dir.resolve("kustomization.yaml");
        Files.createFile(kustomPath);

        Kustomization expected = new Kustomization(kustomPath, Map.of());
        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        when(builder.buildKustomization(kustomPath)).thenReturn(expected);

        ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);
        Stream<GraphNode> result = resolver.resolveDirectory(dir);

        assertEquals(expected, result.findFirst().orElseThrow());
    }

    @Test
    void resolveDirectory_buildsFiles_ifNoKustomization() throws IOException {
        Path dir = tempDir.resolve("only-files");
        Files.createDirectory(dir);
        Path file1 = Files.createFile(dir.resolve("a.yaml"));
        Path file2 = Files.createFile(dir.resolve("b.yaml"));

        KustomFile f1 = new KustomFile(file1);
        KustomFile f2 = new KustomFile(file2);

        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        when(builder.buildKustomFile(file1)).thenReturn(f1);
        when(builder.buildKustomFile(file2)).thenReturn(f2);

        ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);
        Stream<GraphNode> result = resolver.resolveDirectory(dir);

        assertTrue(result.toList().containsAll(Stream.of(f1, f2).toList()));
    }

    @Test
    void resolveDirectory_returnsEmptyStream_forEmptyDir() throws IOException {
        Path dir = tempDir.resolve("empty");
        Files.createDirectory(dir);

        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);

        Stream<GraphNode> result = resolver.resolveDirectory(dir);
        assertTrue(result.toList().isEmpty());
    }
}
