package parser;

import exceptions.InvalidReferenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceValidatorsTest {

    @TempDir
    Path tempDir;

    @Test
    void mustBeFile_acceptsExistingFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("file.yaml"));

        assertDoesNotThrow(() -> ReferenceValidators.mustBeFile().validate(file));
    }

    @Test
    void mustBeFile_rejectsDirectory() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("dir"));

        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeFile().validate(dir)
        );
        assertTrue(ex.getMessage().contains("Expected a file"));
    }

    @Test
    void acceptsValidDirectoryWithKustomizationYaml() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));
        Files.createFile(dir.resolve("kustomization.yaml"));

        assertDoesNotThrow(() ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(dir)
        );
    }

    @Test
    void acceptsValidDirectoryWithKustomizationYml() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));
        Files.createFile(dir.resolve("kustomization.yml"));

        assertDoesNotThrow(() ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(dir)
        );
    }

    @Test
    void acceptsValidKustomizationYamlFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("kustomization.yaml"));

        assertDoesNotThrow(() ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(file)
        );
    }

    @Test
    void acceptsValidKustomizationYmlFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("kustomization.yml"));

        assertDoesNotThrow(() ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(file)
        );
    }

    @Test
    void rejectsIfNotDirectoryOrKustomizationFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("other-file.txt"));

        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(file)
        );
        assertTrue(ex.getMessage().contains("Expected a directory or a kustomization.yaml/yml file"));
    }

    @Test
    void rejectsDirectoryIfMissingKustomizationFile() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));

        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(dir)
        );
        assertTrue(ex.getMessage().contains("Expected a kustomization.yaml or kustomization.yml inside directory"));
    }

    @Test
    void rejectsDirectoryIfMissingBothKustomizationYamlAndYml() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));
        Files.createFile(dir.resolve("some-other-file.txt"));

        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(dir)
        );
        assertTrue(ex.getMessage().contains("Expected a kustomization.yaml or kustomization.yml inside directory"));
    }

    @Test
    void optionalDirectoryOrFile_acceptsExistingFileOrDirectory() throws IOException {
        Path file = Files.createFile(tempDir.resolve("file.yaml"));
        Path dir = Files.createDirectory(tempDir.resolve("dir"));

        assertDoesNotThrow(() -> ReferenceValidators.optionalDirectoryOrFile().validate(file));
        assertDoesNotThrow(() -> ReferenceValidators.optionalDirectoryOrFile().validate(dir));
    }

    @Test
    void optionalDirectoryOrFile_rejectsIfNotExists() {
        Path nonexistent = tempDir.resolve("missing.yaml");

        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.optionalDirectoryOrFile().validate(nonexistent)
        );
        assertTrue(ex.getMessage().contains("Path does not exist"));
    }
}
