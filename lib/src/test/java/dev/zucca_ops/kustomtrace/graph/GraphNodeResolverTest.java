package dev.zucca_ops.kustomtrace.graph;

import dev.zucca_ops.kustomtrace.exceptions.InvalidContentException;
import dev.zucca_ops.kustomtrace.model.KustomFile;
import dev.zucca_ops.kustomtrace.model.KustomResource;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import dev.zucca_ops.kustomtrace.parser.YamlParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

public class GraphNodeResolverTest {
    @TempDir
    Path tempDir;

    @Test
    void parsesMultipleYamlDocuments() throws IOException, InvalidContentException {
        Path filePath = tempDir.resolve("multi.yaml");

        String yaml = """
            kind: ConfigMap
            metadata:
              name: config-1
            ---
            kind: Service
            metadata:
              name: service-1
            """;

        Files.writeString(filePath, yaml);

        KustomFile file = GraphNodeResolver.resolveKustomFile(filePath);
        List<KustomResource> resources = file.getResources();

        assertEquals(2, resources.size());

        KustomResource first = resources.get(0);
        assertEquals("ConfigMap", first.getKind());
        assertEquals("config-1", first.getName());
        assertEquals(filePath, first.getFile().getPath());

        KustomResource second = resources.get(1);
        assertEquals("Service", second.getKind());
        assertEquals("service-1", second.getName());
    }

    @Test
    void returnsEmptyResourcesIfNonYamlExtension() throws IOException, InvalidContentException {
        Path filePath = tempDir.resolve("script.sh");
        Files.writeString(filePath, "echo hello");

        KustomFile file = GraphNodeResolver.resolveKustomFile(filePath);

        assertTrue(file.getResources().isEmpty());
    }

    @Test
    void resolveKustomization_whenParserReturnsValidContent_createsKustomization() throws InvalidContentException, FileNotFoundException {
        Path dummyPath = tempDir.resolve("kustomization.yaml");
        Map<String, Object> mockContent = Map.of("apiVersion", "kustomize.config.k8s.io/v1beta1");

        try (MockedStatic<YamlParser> mockedParser = mockStatic(YamlParser.class)) {
            mockedParser.when(() -> YamlParser.parseKustomizationFile(dummyPath)).thenReturn(mockContent);

            Kustomization kustomization = GraphNodeResolver.resolveKustomization(dummyPath);

            assertNotNull(kustomization);
            assertEquals(dummyPath, kustomization.getPath());
            assertSame(mockContent, kustomization.getContent());
        }
    }
    @Test
    void resolveKustomization_whenParserThrowsInvalidContentException_propagatesException() {
        Path dummyPath = tempDir.resolve("kustomization.yaml");
        InvalidContentException expectedException = new InvalidContentException(dummyPath);

        try (MockedStatic<YamlParser> mockedParser = mockStatic(YamlParser.class)) {
            mockedParser.when(() -> YamlParser.parseKustomizationFile(dummyPath)).thenThrow(expectedException);

            InvalidContentException actualException = assertThrows(InvalidContentException.class, () -> GraphNodeResolver.resolveKustomization(dummyPath));
            assertSame(expectedException, actualException);
        }
    }

}
