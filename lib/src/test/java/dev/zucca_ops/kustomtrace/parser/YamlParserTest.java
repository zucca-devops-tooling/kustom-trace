package dev.zucca_ops.kustomtrace.parser;

import dev.zucca_ops.kustomtrace.exceptions.InvalidContentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class YamlParserTest {

    @TempDir
    Path tempDir;

    @Test
    void parseFile_returnsSingleMap() throws IOException, InvalidContentException {
        Path path = tempDir.resolve("single.yaml");
        Files.writeString(path, "kind: ConfigMap\nmetadata:\n  name: my-config");

        List<Map<String, Object>> docs = YamlParser.parseFile(path);
        assertEquals(1, docs.size());
        assertEquals("ConfigMap", docs.get(0).get("kind"));
    }

    @Test
    void parseFile_parsesMultipleDocuments() throws IOException, InvalidContentException {
        Path path = tempDir.resolve("multi.yaml");
        Files.writeString(path, """
            kind: ConfigMap
            metadata:
              name: one
            ---
            kind: Service
            metadata:
              name: two
            """);

        List<Map<String, Object>> docs = YamlParser.parseFile(path);
        assertEquals(2, docs.size());
        assertEquals("ConfigMap", docs.get(0).get("kind"));
        assertEquals("Service", docs.get(1).get("kind"));
    }

    @Test
    void parseFile_skipsNullDocuments() throws IOException, InvalidContentException {
        Path path = tempDir.resolve("nulls.yaml");
        Files.writeString(path, "---\n---\nkind: ConfigMap");

        List<Map<String, Object>> docs = YamlParser.parseFile(path);
        assertEquals(1, docs.size());
        assertEquals("ConfigMap", docs.get(0).get("kind"));
    }

    @Test
    void parseFile_throwsOnNonMap() throws IOException {
        Path path = tempDir.resolve("list.yaml");
        Files.writeString(path, "- item1\n- item2");

        InvalidContentException ex = assertThrows(InvalidContentException.class, () -> {
            YamlParser.parseFile(path);
        });
        assertTrue(ex.getMessage().contains(path.toString()));
    }

    @Test
    void parseFile_throwsFileNotFoundException() {
        Path path = tempDir.resolve("missing.yaml");

        assertThrows(java.io.FileNotFoundException.class, () -> {
            YamlParser.parseFile(path);
        });
    }

    @Test
    void parseKustomizationFile_returnsSingleDocument() throws IOException, InvalidContentException {
        Path path = tempDir.resolve("kustomization.yaml");
        Files.writeString(path, "resources:\n  - base");

        Map<String, Object> result = YamlParser.parseKustomizationFile(path);
        assertNotNull(result);
        assertTrue(result.containsKey("resources"));
    }

    @Test
    void parseKustomizationFile_returnsNullIfMultipleDocuments() throws IOException, InvalidContentException {
        Path path = tempDir.resolve("invalid.yaml");
        Files.writeString(path, """
            resources:
              - base
            ---
            namePrefix: dev-
            """);

        Map<String, Object> result = YamlParser.parseKustomizationFile(path);
        assertNull(result);
    }

    @Test
    void parseFile_skipsTrailingNullDocument() throws IOException, InvalidContentException {
        Path path = tempDir.resolve("trailing-null.yaml");
        Files.writeString(path, """
        resources:
          - base
        ---
        """);

        List<Map<String, Object>> docs = YamlParser.parseFile(path);
        assertEquals(1, docs.size());
        assertEquals(List.of("base"), docs.get(0).get("resources"));
    }
}
