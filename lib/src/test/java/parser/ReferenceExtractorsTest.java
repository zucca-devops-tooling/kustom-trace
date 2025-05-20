package parser;

import exceptions.InvalidReferenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;


public class ReferenceExtractorsTest {

    @TempDir
    Path baseDir;

    @Test
    void kustomizationFile_resolvesValidKustomizationYaml() throws IOException, InvalidReferenceException {
        Path kustomizationYaml = baseDir.resolve("kustomization.yaml");
        Files.createFile(kustomizationYaml);
        var extractor = ReferenceExtractors.kustomizationFile();
        assertEquals(kustomizationYaml, extractor.extract("kustomization.yaml", baseDir).findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_resolvesValidKustomizationYml() throws IOException, InvalidReferenceException {
        Path kustomizationYml = baseDir.resolve("kustomization.yml");
        Files.createFile(kustomizationYml);
        var extractor = ReferenceExtractors.kustomizationFile();
        assertEquals(kustomizationYml, extractor.extract("kustomization.yml", baseDir).findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_resolvesDirectoryWithKustomizationYaml() throws IOException, InvalidReferenceException {
        Path componentDir = baseDir.resolve("component");
        Files.createDirectory(componentDir);
        Path kustomizationYaml = componentDir.resolve("kustomization.yaml");
        Files.createFile(kustomizationYaml);
        var extractor = ReferenceExtractors.kustomizationFile();
        assertEquals(kustomizationYaml, extractor.extract("component", baseDir).findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_resolvesDirectoryWithKustomizationYml() throws IOException, InvalidReferenceException {
        Path componentDir = baseDir.resolve("component");
        Files.createDirectory(componentDir);
        Path kustomizationYml = componentDir.resolve("kustomization.yml");
        Files.createFile(kustomizationYml);
        var extractor = ReferenceExtractors.kustomizationFile();
        assertEquals(kustomizationYml, extractor.extract("component", baseDir).findFirst().orElse(null));
    }

    @Test
    void kustomizationFile_throwsForNonKustomizationFile() throws IOException {
        Path otherFile = baseDir.resolve("other.yaml");
        Files.createFile(otherFile);
        var extractor = ReferenceExtractors.kustomizationFile();
        assertThrows(InvalidReferenceException.class, () -> extractor.extract("other.yaml", baseDir).findFirst());
    }

    @Test
    void kustomizationFile_throwsForNonString() {
        var extractor = ReferenceExtractors.kustomizationFile();
        assertThrows(InvalidReferenceException.class, () -> extractor.extract(Map.of(), baseDir).findFirst());
    }

    @Test
    void resourceOrDirectory_resolvesValidResourceFile() throws IOException, InvalidReferenceException {
        Path resourceFile = baseDir.resolve("resource.yaml");
        Files.createFile(resourceFile);
        var extractor = ReferenceExtractors.resourceOrDirectory();
        assertEquals(resourceFile, extractor.extract("resource.yaml", baseDir).findFirst().orElse(null));
    }

    @Test
    void resourceOrDirectory_resolvesDirectoryContainingResourceFiles() throws IOException, InvalidReferenceException {
        Path resourceDir = baseDir.resolve("resources");
        Files.createDirectory(resourceDir);
        Path resourceYaml = resourceDir.resolve("data.yaml");
        Path resourceJson = resourceDir.resolve("data.json");
        Files.createFile(resourceYaml);
        Files.createFile(resourceJson);
        var extractor = ReferenceExtractors.resourceOrDirectory();
        List<Path> result = extractor.extract("resources", baseDir).toList();
        assertTrue(result.contains(resourceYaml));
        assertTrue(result.contains(resourceJson));
        assertEquals(2, result.size());
    }

    @Test
    void resourceOrDirectory_throwsForKustomizationFile() throws IOException {
        Path kustomizationYaml = baseDir.resolve("kustomization.yaml");
        Files.createFile(kustomizationYaml);
        var extractor = ReferenceExtractors.resourceOrDirectory();
        assertThrows(InvalidReferenceException.class, () -> extractor.extract("kustomization.yaml", baseDir).findFirst());
    }

    @Test
    void resourceOrDirectory_throwsForNonString() {
        var extractor = ReferenceExtractors.resourceOrDirectory();
        assertThrows(InvalidReferenceException.class, () -> extractor.extract(List.of(), baseDir).findFirst());
    }

    @Test
    void inlinePathValue_extractsSpecifiedField() throws IOException, InvalidReferenceException {
        Path patchFile = baseDir.resolve("patch1.yaml");
        Files.createFile(patchFile);
        var extractor = ReferenceExtractors.inlinePathValue("path");
        Map<String, Object> input = Map.of("path", "patch1.yaml");
        assertEquals(baseDir.resolve("patch1.yaml").normalize(), extractor.extract(input, baseDir).findFirst().orElse(null));
    }

    @Test
    void inlinePathValue_throwsForNonKubernetesResourceFile() {
        var extractor = ReferenceExtractors.inlinePathValue("path");
        Map<String, Object> input = Map.of("path", "other.txt");
        assertThrows(InvalidReferenceException.class, () -> extractor.extract(input, baseDir).findFirst());
    }

    @Test
    void inlinePathValue_returnsEmptyForNonMap() throws InvalidReferenceException {
        var extractor = ReferenceExtractors.inlinePathValue("path");
        assertTrue(extractor.extract("not a map", baseDir).findFirst().isEmpty());
    }

    @Test
    void configMapGeneratorFiles_extractsEnvsAndFiles() throws InvalidReferenceException, IOException {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();
        Path file1 = baseDir.resolve("a.yaml").normalize();
        Path path2 = baseDir.resolve("path/to").normalize();
        Path file2 = path2.resolve("b.yaml").normalize();
        Path file3 = baseDir.resolve("env1").normalize();
        Path file4 = baseDir.resolve("env2").normalize();

        Files.createFile(file1);
        Files.createDirectories(path2);
        Files.createFile(file2);
        Files.createFile(file3);
        Files.createFile(file4);

        Map<String, Object> input = Map.of(
                "files", List.of("a.yaml", "key1=path/to/b.yaml"),
                "envs", List.of("env1", "env2")
        );

        List<Path> result = extractor.extract(input, baseDir).toList();

        assertEquals(4, result.size());
        assertTrue(result.contains(file1));
        assertTrue(result.contains(file2));
        assertTrue(result.contains(file3));
        assertTrue(result.contains(file4));
    }

    @Test
    void configMapGeneratorFiles_returnsEmptyForNonMap() throws InvalidReferenceException {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();
        assertTrue(extractor.extract(List.of(), baseDir).findFirst().isEmpty());
    }
}