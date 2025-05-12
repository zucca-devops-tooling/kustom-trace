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
    void mustBeDirectoryWithKustomizationYaml_acceptsValidDirectory() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));
        Files.createFile(dir.resolve("kustomization.yaml"));

        assertDoesNotThrow(() ->
                ReferenceValidators.mustBeDirectoryWithKustomizationYaml().validate(dir)
        );
    }

    @Test
    void mustBeDirectoryWithKustomizationYaml_rejectsIfNotDirectory() throws IOException {
        Path file = Files.createFile(tempDir.resolve("kustomization.yaml"));

        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeDirectoryWithKustomizationYaml().validate(file)
        );
        assertTrue(ex.getMessage().contains("Expected a directory"));
    }

    @Test
    void mustBeDirectoryWithKustomizationYaml_rejectsIfMissingKustomizationYaml() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));

        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeDirectoryWithKustomizationYaml().validate(dir)
        );
        assertTrue(ex.getMessage().contains("Expected a kustomization.yaml inside directory"));
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
