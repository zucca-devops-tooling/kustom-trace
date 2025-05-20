package graph;

import exceptions.InvalidContentException;
import model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import parser.ReferenceType;

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

    Path rootPath = Paths.get("app/kustomization.yaml");
    Path filePath = Paths.get("apps/config.yaml");

    KustomGraphBuilder builder;

    @BeforeEach
    void setup() {
        builder = new KustomGraphBuilder(Paths.get(".")) {
            {
                try {
                    Field graphField = KustomGraphBuilder.class.getDeclaredField("graph");
                    graphField.setAccessible(true);
                    graphField.set(this, graph);

                    Field resolverField = KustomGraphBuilder.class.getDeclaredField("dependencyResolver");
                    resolverField.setAccessible(true);
                    resolverField.set(this, dependencyResolver);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Test
    void buildsNewKustomizationIfNotPresent_andSetsMutualReferences() throws InvalidContentException, FileNotFoundException {
        when(graph.containsNode(rootPath)).thenReturn(false);

        Kustomization root = new Kustomization(rootPath, Map.of());

        Path filePath = Paths.get("base/config.yaml");
        KustomFile file = new KustomFile(filePath);

        ResourceReference ref = new ResourceReference(ReferenceType.RESOURCE, file);

        try (MockedStatic<GraphNodeResolver> mocked = mockStatic(GraphNodeResolver.class)) {
            mocked.when(() -> GraphNodeResolver.resolveKustomization(rootPath)).thenReturn(root);
            when(dependencyResolver.resolveDependencies(root)).thenReturn(Stream.of(ref));
            when(graph.getNode(rootPath)).thenReturn(root);

            Kustomization result = builder.buildKustomization(rootPath);

            assertEquals(root, result);
            verify(graph).addNode(root);

            // Reference is registered in root
            assertEquals(1, root.getReferences().size());
            assertSame(ref, root.getReferences().get(0));

            // root is registered as a dependent in file
            assertTrue(
                    file.hasDependent(root),
                    "Root should appear in apps stream of the referenced file node"
            );
        }
    }

    @Test
    void returnsExistingKustomizationIfAlreadyInGraph() throws InvalidContentException, FileNotFoundException {
        Kustomization existing = new Kustomization(rootPath, Map.of());

        when(graph.containsNode(rootPath)).thenReturn(true);
        when(graph.getNode(rootPath)).thenReturn(existing);

        Kustomization result = builder.buildKustomization(rootPath);

        assertEquals(existing, result);
        verify(graph, never()).addNode(any());
    }

    @Test
    void returnsExistingKustomFileIfAlreadyInGraph() throws InvalidContentException, FileNotFoundException {
        KustomFile existing = new KustomFile(filePath);

        when(graph.containsNode(filePath)).thenReturn(true);
        when(graph.getNode(filePath)).thenReturn(existing);

        KustomFile result = builder.buildKustomFile(filePath);

        assertSame(existing, result);
        verify(graph, never()).addNode(any());
    }

    @Test
    void buildsNewKustomFileIfNotInGraph() throws InvalidContentException, FileNotFoundException {
        KustomFile parsed = new KustomFile(filePath);

        when(graph.containsNode(filePath)).thenReturn(false);

        try (MockedStatic<GraphNodeResolver> mocked = mockStatic(GraphNodeResolver.class)) {
            mocked.when(() -> GraphNodeResolver.resolveKustomFile(filePath)).thenReturn(parsed);
            when(graph.getNode(filePath)).thenReturn(parsed);

            KustomFile result = builder.buildKustomFile(filePath);

            assertSame(parsed, result);
            verify(graph).addNode(parsed);
        }
    }
}
