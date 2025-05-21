package dev.zucca_ops.kustomtrace.graph;

import dev.zucca_ops.kustomtrace.exceptions.InvalidContentException;
import dev.zucca_ops.kustomtrace.model.KustomFile;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import dev.zucca_ops.kustomtrace.model.ResourceReference;
import dev.zucca_ops.kustomtrace.parser.KustomizeFileUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import dev.zucca_ops.kustomtrace.parser.ReferenceType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ResourceReferenceResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolveDependency_buildsKustomization_forValidKustomizationFile() throws IOException, InvalidContentException {
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
    void resolveDependency_buildsKustomFile_forNonKustomizationFile() throws IOException, InvalidContentException {
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
    void resolveDependencies_processesValidReferences() throws IOException, InvalidContentException {
        // 1. Setup Paths and Files
        Path kustomPath = tempDir.resolve("kustomization.yaml");
        Files.writeString(kustomPath, "components:\n  - component1\nresources:\n  - resource.yaml");

        Path componentDir = tempDir.resolve("component1");
        Files.createDirectory(componentDir);
        Path componentKustomizationPath = componentDir.resolve("kustomization.yaml");
        Files.createFile(componentKustomizationPath); // Create the component's kustomization file

        Path resourcePath = tempDir.resolve("resource.yaml");
        Files.createFile(resourcePath); // Create the resource file

        // 2. Create Root Kustomization and Mock Collaborators
        Kustomization root = new Kustomization(kustomPath, Map.of("components", List.of("component1"), "resources", List.of("resource.yaml")));

        // Mock KustomGraphBuilder, as ResourceReferenceResolver depends on it
        KustomGraphBuilder mockedBuilder = mock(KustomGraphBuilder.class);
        ResourceReferenceResolver resolver = new ResourceReferenceResolver(mockedBuilder);

        // Define the objects that the mocked builder should return
        Kustomization componentNode = new Kustomization(componentKustomizationPath, Map.of());
        KustomFile resourceNode = new KustomFile(resourcePath);

        // Stub the builder's methods
        when(mockedBuilder.buildKustomization(componentKustomizationPath)).thenReturn(componentNode);
        when(mockedBuilder.buildKustomFile(resourcePath)).thenReturn(resourceNode);

        // 3. Execute the method under test
        Stream<ResourceReference> resultStream = resolver.resolveDependencies(root);
        List<ResourceReference> references = resultStream.toList();

        // 4. Assertions (Order-Independent)

        // 4a. Assert the total number of references
        assertEquals(2, references.size(), "Should have resolved exactly two references.");

        // 4b. Verify that the expected COMPONENT reference is present
        ReferenceType expectedComponentType = ReferenceType.COMPONENT;
        Kustomization expectedComponentInstance = componentNode; // The exact instance from mock setup

        long componentReferenceCount = references.stream()
                .filter(ref -> ref.referenceType() == expectedComponentType && ref.resource() == expectedComponentInstance)
                .count();
        assertEquals(1, componentReferenceCount, "Exactly one COMPONENT reference to the expected component instance (" + componentKustomizationPath.getFileName() + ") should exist.");

        // 4c. Verify that the expected RESOURCE reference is present
        ReferenceType expectedResourceType = ReferenceType.RESOURCE;
        KustomFile expectedResourceInstance = resourceNode; // The exact instance from mock setup

        long resourceReferenceCount = references.stream()
                .filter(ref -> ref.referenceType() == expectedResourceType && ref.resource() == expectedResourceInstance)
                .count();
        assertEquals(1, resourceReferenceCount, "Exactly one RESOURCE reference to the expected resource instance (" + resourcePath.getFileName() + ") should exist.");

        // 5. Verify interactions with the mocked builder (these remain the same)
        verify(mockedBuilder).buildKustomization(componentKustomizationPath);
        verify(mockedBuilder).buildKustomFile(resourcePath);
    }

    @Test
    void returnsNullOnInvalidContent() throws InvalidContentException, FileNotFoundException {
        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        try (MockedStatic<KustomizeFileUtil> mocked = mockStatic(KustomizeFileUtil.class)) {
            mocked.when(() -> KustomizeFileUtil.isKustomizationFileName(any())).thenReturn(true);
            when(builder.buildKustomization(any())).thenThrow(new InvalidContentException(Path.of("any")));

            ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);
            ResourceReference result = resolver.resolveDependency(ReferenceType.RESOURCE, mock());

            assertNull(result);
        }
    }

    @Test
    void returnsNullOnFileNotFound() throws InvalidContentException, FileNotFoundException {
        KustomGraphBuilder builder = mock(KustomGraphBuilder.class);
        try (MockedStatic<KustomizeFileUtil> mocked = mockStatic(KustomizeFileUtil.class)) {
            mocked.when(() -> KustomizeFileUtil.isKustomizationFileName(any())).thenReturn(true);
            when(builder.buildKustomization(any())).thenThrow(new FileNotFoundException("any"));

            ResourceReferenceResolver resolver = new ResourceReferenceResolver(builder);
            ResourceReference result = resolver.resolveDependency(ReferenceType.RESOURCE, mock());

            assertNull(result);
        }
    }
}