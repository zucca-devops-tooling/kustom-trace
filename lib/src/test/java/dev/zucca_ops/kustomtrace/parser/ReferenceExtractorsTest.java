package dev.zucca_ops.kustomtrace.parser;

import dev.zucca_ops.kustomtrace.exceptions.InvalidReferenceException;
import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;


public class ReferenceExtractorsTest {

    @TempDir
    Path baseDir; // This will be the parent for most test files and for 'baseDir' arg
    @Nested
    @DisplayName("directory() Extractor Tests")
    class DirectoryExtractorTests {

        @Test
        void directory_resolvesToKustomizationYaml_InTargetDir() throws IOException, InvalidReferenceException {
            Path targetDir = Files.createDirectory(baseDir.resolve("componentA"));
            Path expectedKustomization = targetDir.resolve("kustomization.yaml");
            Files.createFile(expectedKustomization);

            ReferenceExtractor extractor = ReferenceExtractors.directory();
            Optional<Path> result = extractor.extract("componentA", baseDir).findFirst();

            assertTrue(result.isPresent());
            assertEquals(expectedKustomization.toAbsolutePath().normalize(), result.get().toAbsolutePath().normalize());
        }

        @Test
        void directory_resolvesToKustomizationYml_IfYamlNotPresent() throws IOException, InvalidReferenceException {
            Path targetDir = Files.createDirectory(baseDir.resolve("componentB"));
            Path expectedKustomization = targetDir.resolve("kustomization.yml");
            Files.createFile(expectedKustomization);

            ReferenceExtractor extractor = ReferenceExtractors.directory();
            Optional<Path> result = extractor.extract("componentB", baseDir).findFirst();

            assertTrue(result.isPresent());
            assertEquals(expectedKustomization.toAbsolutePath().normalize(), result.get().toAbsolutePath().normalize());
        }

        @Test
        void directory_resolvesToKustomizationUpper_IfYamlAndYmlNotPresent() throws IOException, InvalidReferenceException {
            Path targetDir = Files.createDirectory(baseDir.resolve("componentC"));
            Path expectedKustomization = targetDir.resolve("Kustomization");
            Files.createFile(expectedKustomization);

            ReferenceExtractor extractor = ReferenceExtractors.directory();
            Optional<Path> result = extractor.extract("componentC", baseDir).findFirst();

            assertTrue(result.isPresent());
            assertEquals(expectedKustomization.toAbsolutePath().normalize(), result.get().toAbsolutePath().normalize());
        }


        @Test
        void directory_throwsInvalidReferenceException_ifTargetIsNotDirectory() throws IOException {
            Path targetFile = Files.createFile(baseDir.resolve("not_a_directory.yaml"));
            ReferenceExtractor extractor = ReferenceExtractors.directory();

            InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () -> {
                extractor.extract("not_a_directory.yaml", baseDir).findFirst();
            });
            assertTrue(ex.getMessage().contains("Expected a directory"));
        }

        @Test
        void directory_throwsInvalidReferenceException_ifDirectoryHasNoKustomizationFile() throws IOException {
            Path targetDir = Files.createDirectory(baseDir.resolve("emptyComponent"));
            ReferenceExtractor extractor = ReferenceExtractors.directory();

            InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () -> {
                extractor.extract("emptyComponent", baseDir).findFirst();
            });
            assertTrue(ex.getMessage().contains("Expected directory with Kustomization inside"));
            // Check that the cause is NotAnAppException
            assertNotNull(ex.getCause());
            assertTrue(ex.getCause() instanceof NotAnAppException);
        }

        @Test
        void directory_throwsForSelfReference() {
            ReferenceExtractor extractor = ReferenceExtractors.directory();
            // Kustomize disallows "." as a kustomization path in 'bases' or 'components'
            // if it means self-reference that leads to issues.
            // Your extractKustomization helper handles this.
            assertThrows(InvalidReferenceException.class, () -> extractor.extract(".", baseDir).findFirst());
        }
    }

    @Nested
    @DisplayName("resourceOrDirectory() Extractor Tests")
    class ResourceOrDirectoryExtractorTests {

        @Test
        void resourceOrDirectory_resolvesToKustomizationFile_ifTargetIsDirWithKustomization() throws IOException, InvalidReferenceException {
            Path targetDir = Files.createDirectory(baseDir.resolve("appA"));
            Path expectedKustomization = targetDir.resolve("kustomization.yaml");
            Files.createFile(expectedKustomization);

            ReferenceExtractor extractor = ReferenceExtractors.resourceOrDirectory();
            Optional<Path> result = extractor.extract("appA", baseDir).findFirst();

            assertTrue(result.isPresent());
            assertEquals(expectedKustomization.toAbsolutePath().normalize(), result.get().toAbsolutePath().normalize());
        }

        @Test
        void resourceOrDirectory_resolvesToResourceFile_ifTargetIsFile() throws IOException, InvalidReferenceException {
            Path targetFile = Files.createFile(baseDir.resolve("service.yaml"));
            ReferenceExtractor extractor = ReferenceExtractors.resourceOrDirectory();
            Optional<Path> result = extractor.extract("service.yaml", baseDir).findFirst();

            assertTrue(result.isPresent());
            assertEquals(targetFile.toAbsolutePath().normalize(), result.get().toAbsolutePath().normalize());
        }

        @Test
        void resourceOrDirectory_throwsIfFileIsKustomizationNameButNotKustomizationTarget() throws IOException {
            // e.g. resources: [kustomization.yaml] where kustomization.yaml is a resource file
            // KustomizeFileUtil.getKustomizationFileFromAppDirectory would resolve this IF it exists and IS a kustomization.
            // If it's meant to be a plain resource but named kustomization.yaml, it's tricky.
            // Your current resourceOrDirectory logic:
            // 1. Tries KustomizeFileUtil.getKustomizationFileFromAppDirectory(path).
            // 2. If NotAnAppException, then validateKubernetesResource(path).
            // KustomizeFileUtil.isValidKubernetesResource returns false for "kustomization.yaml".
            // So validateKubernetesResource will throw "Not a valid Kubernetes resource".

            Path kustomizationAsResource = baseDir.resolve("kustomization.yaml");
            Files.createFile(kustomizationAsResource); // It exists

            ReferenceExtractor extractor = ReferenceExtractors.resourceOrDirectory();
            InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () -> {
                extractor.extract("kustomization.yaml", baseDir).toList();
            });
            assertTrue(ex.getMessage().contains("Not a valid Kubernetes resource"));
        }

        @Test
        void resourceOrDirectory_throwsIfDirectoryHasNoKustomizationAndNotValidResourceFileItself() throws IOException {
            Path plainDir = Files.createDirectory(baseDir.resolve("plainDir"));
            ReferenceExtractor extractor = ReferenceExtractors.resourceOrDirectory();

            InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () -> {
                extractor.extract("plainDir", baseDir).toList();
            });
            assertTrue(ex.getMessage().contains("Expected directory with Kustomization inside"));
        }

        @Test
        void resourceOrDirectory_throwsIfNonExistentFile() {
            ReferenceExtractor extractor = ReferenceExtractors.resourceOrDirectory();
            // KFG.getKustomizationFileFromAppDirectory will throw for non-existent path.
            // That NotAnAppException is caught, then validateKubernetesResource is called.
            // validateKubernetesResource will then throw because KustomizeFileUtil.isFile will be false.
            InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () -> {
                extractor.extract("nonexistent.yaml", baseDir).toList();
            });
            assertTrue(ex.getMessage().contains("Non-existing or non-regular file referenced"));
        }
    }

    @Test
    void resource_resolvesValidResourceFile() throws IOException, InvalidReferenceException {
        Path resourceFile = baseDir.resolve("resource.yaml");
        Files.createFile(resourceFile);
        var extractor = ReferenceExtractors.resource();
        assertEquals(resourceFile.toAbsolutePath().normalize(), extractor.extract("resource.yaml", baseDir).findFirst().orElse(null).toAbsolutePath().normalize());
    }

    @Test
    void resource_throwsForNonExistentFile() {
        var extractor = ReferenceExtractors.resource();
        assertThrows(InvalidReferenceException.class, () -> extractor.extract("nonexistent.yaml", baseDir).findFirst());
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
    void inlinePathValue_throwsForNonKubernetesResourceFile() throws IOException {
        Path otherFile = baseDir.resolve("other.txt");
        Files.createFile(otherFile); // Create the file so isFile is true, but isValidKubernetesResource is false
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
    void inlinePathValue_returnsEmptyIfPathFieldMissing() throws InvalidReferenceException {
        var extractor = ReferenceExtractors.inlinePathValue("path");
        Map<String, Object> input = Map.of("otherField", "somevalue.yaml");
        assertTrue(extractor.extract(input, baseDir).findFirst().isEmpty(), "Should return empty stream if path field is missing.");
    }

    @Test
    void configMapGeneratorFiles_extractsEnvsAndFiles() throws InvalidReferenceException, IOException {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();
        Path file1 = baseDir.resolve("a.yaml"); // Will be filtered by isNotKustomizationFile if KustomizeFileUtil.isKustomizationFileName matches based on content.
        // Assuming a.yaml is NOT "kustomization.yaml"
        Path pathToB = baseDir.resolve("path").resolve("to"); // Intermediate dir
        Files.createDirectories(pathToB);
        Path file2 = pathToB.resolve("b.yaml");
        Path file3 = baseDir.resolve("env1.env"); // Changed extension for clarity as resource
        Path file4 = baseDir.resolve("env2.properties"); // Changed extension

        // KustomizeFileUtil.isFile needs these to exist
        Files.createFile(file1);
        Files.createFile(file2);
        Files.createFile(file3);
        Files.createFile(file4);

        Map<String, Object> input = Map.of(
                "files", List.of("a.yaml", "key1=path/to/b.yaml"),
                "envs", List.of("env1.env", "env2.properties")
        );

        List<Path> result = extractor.extract(input, baseDir)
                .map(p -> p.toAbsolutePath().normalize()) // Normalize for comparison
                .toList();


        assertEquals(4, result.size(), "Should find 4 files");
        assertTrue(result.contains(file1.toAbsolutePath().normalize()));
        assertTrue(result.contains(file2.toAbsolutePath().normalize()));
        assertTrue(result.contains(file3.toAbsolutePath().normalize()));
        assertTrue(result.contains(file4.toAbsolutePath().normalize()));
    }

    @Test
    void configMapGeneratorFiles_skipsKustomizationFiles() throws InvalidReferenceException, IOException {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();
        Path kustomizationFile = baseDir.resolve("kustomization.yaml");
        Path envFile = baseDir.resolve("env.env");

        Files.createFile(kustomizationFile);
        Files.createFile(envFile);

        Map<String, Object> input = Map.of(
                "files", List.of("kustomization.yaml"), // This should be skipped
                "envs", List.of("env.env")            // This should be included
        );
        List<Path> result = extractor.extract(input, baseDir).toList();
        assertEquals(1, result.size());
        assertTrue(result.contains(envFile.toAbsolutePath().normalize()));
        assertFalse(result.contains(kustomizationFile.toAbsolutePath().normalize()));
    }
}