package dev.zucca_ops.kustomtrace.graph;

import dev.zucca_ops.kustomtrace.exceptions.InvalidContentException;
import dev.zucca_ops.kustomtrace.model.KustomFile;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import dev.zucca_ops.kustomtrace.model.ResourceReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import dev.zucca_ops.kustomtrace.parser.ReferenceType;

import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KustomGraphBuilderTest {
    @Mock
    KustomGraph graph;

    @Mock
    ResourceReferenceResolver dependencyResolver;

    Path relativeRootPath = Paths.get("app/kustomization.yaml");
    Path relativeFilePathForKustomFile = Paths.get("apps/config.yaml");

    Path absoluteRootPath;
    Path absoluteFilePathForKustomFile;

    KustomGraphBuilder builder;

    @BeforeEach
    void setup() {
        Path appsDir = Paths.get(".");
        builder = new KustomGraphBuilder(appsDir) {
            {
                try {
                    Field graphField = KustomGraphBuilder.class.getDeclaredField("graph");
                    graphField.setAccessible(true);
                    graphField.set(this, KustomGraphBuilderTest.this.graph);

                    Field resolverField = KustomGraphBuilder.class.getDeclaredField("dependencyResolver");
                    resolverField.setAccessible(true);
                    resolverField.set(this, KustomGraphBuilderTest.this.dependencyResolver);

                } catch (Exception e) {
                    throw new RuntimeException("Failed to inject mocks into KustomGraphBuilder via reflection", e);
                }
            }
        };

        Path builderAppsDir = appsDir.toAbsolutePath().normalize();

        absoluteRootPath = builderAppsDir.resolve(relativeRootPath).normalize();
        absoluteFilePathForKustomFile = builderAppsDir.resolve(relativeFilePathForKustomFile).normalize();
    }

    @Test
    void buildsNewKustomizationIfNotPresent_andSetsMutualReferences() throws InvalidContentException, FileNotFoundException {
        Path basePathForTest = Paths.get("").toAbsolutePath();
        Path relativeRootPathForTest = Paths.get("app/kustomization.yaml"); // Path passed to buildKustomization
        Path absoluteRootPath = basePathForTest.resolve(relativeRootPathForTest).normalize();

        Path contentFilePath = Paths.get("base/config.yaml");
        Path absoluteDependencyPath = absoluteRootPath.getParent().resolve(contentFilePath).normalize();

        // --- Stubs ---
        when(graph.containsNode(absoluteRootPath)).thenReturn(false); // This drives the logic path

        Kustomization root = new Kustomization(absoluteRootPath, Map.of());
        KustomFile file = new KustomFile(absoluteDependencyPath);
        ResourceReference ref = new ResourceReference(ReferenceType.RESOURCE, file);

        try (MockedStatic<GraphNodeResolver> mockedGNR = mockStatic(GraphNodeResolver.class)) {
            mockedGNR.when(() -> GraphNodeResolver.resolveKustomization(absoluteRootPath)).thenReturn(root);
            when(dependencyResolver.resolveDependencies(root)).thenReturn(Stream.of(ref));

            // --- Execute ---
            Kustomization result = builder.buildKustomization(relativeRootPathForTest);

            // --- Assertions ---
            assertEquals(root, result);
            verify(graph).addNode(root); // Verify addNode is called with the Kustomization object

            assertEquals(1, root.getReferences().size());
            assertSame(ref, root.getReferences().get(0));

            assertTrue(
                    file.hasDependent(root),
                    "Root should appear as a dependent in the referenced file node"
            );
        }
    }

    @Test
    void returnsExistingKustomizationIfAlreadyInGraph() throws InvalidContentException, FileNotFoundException {
        Kustomization existing = new Kustomization(absoluteRootPath, Map.of());

        when(graph.containsNode(absoluteRootPath)).thenReturn(true);
        when(graph.getKustomization(absoluteRootPath)).thenReturn(existing);

        Kustomization result = builder.buildKustomization(relativeRootPath); // Pass relative, builder normalizes

        assertEquals(existing, result);
        verify(graph, never()).addNode(any());
    }

    @Test
    void returnsExistingKustomFileIfAlreadyInGraph() throws InvalidContentException, FileNotFoundException {
        KustomFile existing = new KustomFile(absoluteFilePathForKustomFile);

        when(graph.containsNode(absoluteFilePathForKustomFile)).thenReturn(true);
        when(graph.getKustomFile(absoluteFilePathForKustomFile)).thenReturn(existing);

        KustomFile result = builder.buildKustomFile(relativeFilePathForKustomFile); // Pass relative, builder normalizes

        assertSame(existing, result);
        verify(graph, never()).addNode(any());
    }

    @Test
    void buildsNewKustomFileIfNotInGraph() throws InvalidContentException, FileNotFoundException {
        KustomFile parsed = new KustomFile(absoluteFilePathForKustomFile);

        // --- Stubs ---
        when(graph.containsNode(absoluteFilePathForKustomFile)).thenReturn(false);

        try (MockedStatic<GraphNodeResolver> mockedGNR = mockStatic(GraphNodeResolver.class)) {
            mockedGNR.when(() -> GraphNodeResolver.resolveKustomFile(absoluteFilePathForKustomFile)).thenReturn(parsed);

            // --- Execute ---
            KustomFile result = builder.buildKustomFile(relativeFilePathForKustomFile);

            // --- Assertions ---
            assertSame(parsed, result);
            verify(graph).addNode(parsed);
        }
    }
}