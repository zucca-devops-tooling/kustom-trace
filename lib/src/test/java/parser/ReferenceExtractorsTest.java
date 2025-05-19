package parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ReferenceExtractorsTest {

    @TempDir
    Path baseDir;

    @Test
    void kustomizationFile_resolvesValidKustomizationYaml() throws IOException {
        Path kustomizationYaml = baseDir.resolve("kustomization.yaml");
        Files.createFile(kustomizationYaml);
        var extractor = ReferenceExtractors.kustomizationFile();
        Stream<Path> result = extractor.extract("kustomization.yaml", baseDir);
        assertEquals(kustomizationYaml, result.findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_resolvesValidKustomizationYml() throws IOException {
        Path kustomizationYml = baseDir.resolve("kustomization.yml");
        Files.createFile(kustomizationYml);
        var extractor = ReferenceExtractors.kustomizationFile();
        Stream<Path> result = extractor.extract("kustomization.yml", baseDir);
        assertEquals(kustomizationYml, result.findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_resolvesDirectoryWithKustomizationYaml() throws IOException {
        Path componentDir = baseDir.resolve("component");
        Files.createDirectory(componentDir);
        Path kustomizationYaml = componentDir.resolve("kustomization.yaml");
        Files.createFile(kustomizationYaml);
        var extractor = ReferenceExtractors.kustomizationFile();
        Stream<Path> result = extractor.extract("component", baseDir);
        assertEquals(kustomizationYaml, result.findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_resolvesDirectoryWithKustomizationYml() throws IOException {
        Path componentDir = baseDir.resolve("component");
        Files.createDirectory(componentDir);
        Path kustomizationYml = componentDir.resolve("kustomization.yml");
        Files.createFile(kustomizationYml);
        var extractor = ReferenceExtractors.kustomizationFile();
        Stream<Path> result = extractor.extract("component", baseDir);
        assertEquals(kustomizationYml, result.findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_returnsEmptyForNonKustomizationFile() throws IOException {
        Path otherFile = baseDir.resolve("other.yaml");
        Files.createFile(otherFile);
        var extractor = ReferenceExtractors.kustomizationFile();
        Stream<Path> result = extractor.extract("other.yaml", baseDir);
        assertTrue(result.findFirst().isEmpty());
    }

    @Test
    void kustomizationFile_returnsEmptyForNonString() {
        var extractor = ReferenceExtractors.kustomizationFile();
        Stream<Path> result = extractor.extract(Map.of(), baseDir);
        assertTrue(result.findFirst().isEmpty());
    }

    @Test
    void resource_resolvesValidResourceFile() throws IOException {
        Path resourceFile = baseDir.resolve("resource.yaml");
        Files.createFile(resourceFile);
        var extractor = ReferenceExtractors.resource();
        Stream<Path> result = extractor.extract("resource.yaml", baseDir);
        assertEquals(resourceFile, result.findFirst().orElse(null));
    }

    @Test
    void resource_resolvesDirectoryContainingResourceFiles() throws IOException {
        Path resourceDir = baseDir.resolve("resources");
        Files.createDirectory(resourceDir);
        Path resourceYaml = resourceDir.resolve("data.yaml");
        Path resourceJson = resourceDir.resolve("data.json");
        Files.createFile(resourceYaml);
        Files.createFile(resourceJson);
        var extractor = ReferenceExtractors.resource();
        List<Path> result = extractor.extract("resources", baseDir).toList();
        assertTrue(result.contains(resourceYaml));
        assertTrue(result.contains(resourceJson));
        assertEquals(2, result.size());
    }

    @Test
    void resource_returnsEmptyForKustomizationFile() throws IOException {
        Path kustomizationYaml = baseDir.resolve("kustomization.yaml");
        Files.createFile(kustomizationYaml);
        var extractor = ReferenceExtractors.resource();
        Stream<Path> result = extractor.extract("kustomization.yaml", baseDir);
        assertTrue(result.findFirst().isEmpty());
    }

    @Test
    void resource_returnsEmptyForNonString() {
        var extractor = ReferenceExtractors.resource();
        Stream<Path> result = extractor.extract(List.of(), baseDir);
        assertTrue(result.findFirst().isEmpty());
    }

    @Test
    void inlinePathValue_extractsSpecifiedField() {
        var extractor = ReferenceExtractors.inlinePathValue("path");

        List<Map<String, Object>> input = List.of(
                Map.of("path", "patch1.yaml"),
                Map.of("path", "patch2.yaml"),
                Map.of("nope", "ignored")
        );

        Stream<Path> result = extractor.extract(input.get(0), baseDir);
        assertEquals(baseDir.resolve("patch1.yaml").normalize(), result.findFirst().orElse(null));

        result = extractor.extract(input.get(2), baseDir);
        assertTrue(result.findFirst().isEmpty());
    }

    @Test
    void inlinePathValue_returnsEmptyForNonMap() {
        var extractor = ReferenceExtractors.inlinePathValue("path");
        Stream<Path> result = extractor.extract("not a map", baseDir);
        assertTrue(result.findFirst().isEmpty());
    }

    @Test
    void configMapGeneratorFiles_extractsEnvsAndFiles() {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();

        Map<String, Object> input = Map.of(
                "files", List.of("a.yaml", "key1=path/to/b.yaml"),
                "envs", List.of("env1", "env2")
        );

        List<Path> result = extractor.extract(input, baseDir).toList();

        assertEquals(4, result.size());
        assertTrue(result.contains(baseDir.resolve("a.yaml").normalize()));
        assertTrue(result.contains(baseDir.resolve("path/to/b.yaml").normalize()));
        assertTrue(result.contains(baseDir.resolve("env1").normalize()));
        assertTrue(result.contains(baseDir.resolve("env2").normalize()));
    }

    @Test
    void configMapGeneratorFiles_returnsEmptyForNonMap() {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();
        Stream<Path> result = extractor.extract(List.of(), baseDir);
        assertTrue(result.findFirst().isEmpty());
    }
}