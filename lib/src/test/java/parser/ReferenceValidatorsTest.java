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
    void mustBeKubernetesResource_acceptsExistingYamlFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("resource.yaml"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeKubernetesResource().validate(file));
    }

    @Test
    void mustBeKubernetesResource_acceptsExistingYmlFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("resource.yml"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeKubernetesResource().validate(file));
    }

    @Test
    void mustBeKubernetesResource_acceptsExistingJsonFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("resource.json"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeKubernetesResource().validate(file));
    }

    @Test
    void mustBeKubernetesResource_acceptsExistingDirectory() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("dir"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeKubernetesResource().validate(dir));
    }

    @Test
    void mustBeKubernetesResource_rejectsNonResourceFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("other.txt"));
        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeKubernetesResource().validate(file)
        );
        assertTrue(ex.getMessage().contains("Expected a .yaml, .yml, or .json file"));
    }

    @Test
    void mustBeDirectoryOrKustomizationFile_acceptsValidDirectoryWithKustomizationYaml() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));
        Files.createFile(dir.resolve("kustomization.yaml"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(dir));
    }

    @Test
    void mustBeDirectoryOrKustomizationFile_acceptsValidDirectoryWithKustomizationYml() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));
        Files.createFile(dir.resolve("kustomization.yml"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(dir));
    }

    @Test
    void mustBeDirectoryOrKustomizationFile_acceptsValidKustomizationYamlFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("kustomization.yaml"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(file));
    }

    @Test
    void mustBeDirectoryOrKustomizationFile_acceptsValidKustomizationYmlFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("kustomization.yml"));
        assertDoesNotThrow(() -> ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(file));
    }

    @Test
    void mustBeDirectoryOrKustomizationFile_rejectsNonKustomizationFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("other.yaml"));
        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(file)
        );
        assertTrue(ex.getMessage().contains("Expected a kustomization.yaml/yml file"));
    }

    @Test
    void mustBeDirectoryOrKustomizationFile_rejectsDirectoryIfMissingKustomizationFile() throws IOException {
        Path dir = Files.createDirectory(tempDir.resolve("component"));
        InvalidReferenceException ex = assertThrows(InvalidReferenceException.class, () ->
                ReferenceValidators.mustBeDirectoryOrKustomizationFile().validate(dir)
        );
        assertTrue(ex.getMessage().contains("Expected a kustomization.yaml or kustomization.yml inside directory"));
    }
}