package graph;

import exceptions.InvalidReferenceException;
import model.GraphNode;
import model.KustomFile;
import model.Kustomization;
import model.ResourceReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import parser.ReferenceType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    void resolveDependency_buildsKustomization_forValidKustomizationFile() throws IOException {
        Path kustomPath = tempDir.resolve("kustomization.yaml");
        Files.createFile(kustomPath);

        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        Kustomization expectedKustomization = new Kustomization(kustomPath, Map.of());
        when(builder.buildKustomization(kustomPath)).thenReturn(expectedKustomization);

        ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);
        ResourceReference result = resolver.resolveDependency(ReferenceType.BASE, kustomPath);

        assertEquals(expectedKustomization, result.resource());
        assertEquals(ReferenceType.BASE, result.referenceType());
        verify(builder).buildKustomization(kustomPath);
        verify(builder, never()).buildKustomFile(any());
    }

    @Test
    void resolveDependency_buildsKustomFile_forNonKustomizationFile() throws IOException {
        Path filePath = tempDir.resolve("config.yaml");
        Files.writeString(filePath, "kind: ConfigMap");

        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        KustomFile expectedFile = new KustomFile(filePath);
        when(builder.buildKustomFile(filePath)).thenReturn(expectedFile);

        ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);
        ResourceReference result = resolver.resolveDependency(ReferenceType.RESOURCE, filePath);

        assertEquals(expectedFile, result.resource());
        assertEquals(ReferenceType.RESOURCE, result.referenceType());
        verify(builder).buildKustomFile(filePath);
        verify(builder, never()).buildKustomization(any());
    }

    @Test
    void resolveDependencies_processesValidReferences() throws IOException {
        Path kustomPath = tempDir.resolve("kustomization.yaml");
        Files.writeString(kustomPath, "components:\n  - component1\nresources:\n  - resource.yaml");

        Path componentDir = tempDir.resolve("component1");
        Files.createDirectory(componentDir);
        Path componentKustomizationPath = componentDir.resolve("kustomization.yaml");
        Files.createFile(componentKustomizationPath); // Create the component's kustomization file

        Path resourcePath = tempDir.resolve("resource.yaml");
        Files.createFile(resourcePath); // Create the resource file

        Kustomization root = new Kustomization(kustomPath, Map.of("components", List.of("component1"), "resources", List.of("resource.yaml")));
        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);

        Kustomization componentNode = new Kustomization(componentKustomizationPath, Map.of());
        KustomFile resourceNode = new KustomFile(resourcePath);

        when(builder.buildKustomization(componentKustomizationPath)).thenReturn(componentNode);
        when(builder.buildKustomFile(resourcePath)).thenReturn(resourceNode);

        Stream<ResourceReference> result = resolver.resolveDependencies(root);
        List<ResourceReference> references = result.toList();

        assertEquals(2, references.size());
        assertEquals(ReferenceType.COMPONENT, references.get(0).referenceType());
        assertEquals(componentNode, references.get(0).resource());
        assertEquals(ReferenceType.RESOURCE, references.get(1).referenceType());
        assertEquals(resourceNode, references.get(1).resource());

        verify(builder).buildKustomization(componentKustomizationPath);
        verify(builder).buildKustomFile(resourcePath);
    }
}