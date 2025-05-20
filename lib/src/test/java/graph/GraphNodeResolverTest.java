package graph;

import exceptions.InvalidContentException;
import model.KustomFile;
import model.KustomResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import parser.YamlParser;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    void throwsExceptionWhenContentIsNull() {
        Path path = tempDir.resolve("kustomization.yaml");

        try (MockedStatic<YamlParser> mocked = mockStatic(YamlParser.class)) {
            mocked.when(() -> YamlParser.parseKustomizationFile(path)).thenReturn(null);

            assertThrows(InvalidContentException.class, () -> {
                GraphNodeResolver.resolveKustomization(path);
            });
        }
    }
}
