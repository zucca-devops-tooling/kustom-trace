package parser;

import exceptions.InvalidReferenceException;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReferenceValidators {

    public static ReferenceValidator mustBeFile() {
        return path -> {
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new InvalidReferenceException("Expected a file", path);
            }
        };
    }

    public static ReferenceValidator mustBeDirectoryWithKustomizationYaml() {
        return path -> {
            if (!Files.isDirectory(path)) {
                throw new InvalidReferenceException("Expected a directory", path);
            }
            Path kustomization = path.resolve("kustomization.yaml");
            if (!Files.exists(kustomization)) {
                throw new InvalidReferenceException("Expected a kustomization.yaml inside directory", path);
            }
        };
    }

    public static ReferenceValidator optionalDirectoryOrFile() {
        return path -> {
            if (!Files.exists(path)) {
                throw new InvalidReferenceException("Path does not exist", path);
            }
        };
    }
}