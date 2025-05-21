package dev.zucca_ops.kustomtrace.parser;

import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class KustomizeFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(KustomizeFileUtil.class);


    /**
     * Checks if the given path points to a file that is a regular file and exists.
     * @param path the path to check.
     * @return true if the path exists and is a regular file, false otherwise.
     */
    public static boolean isFile(Path path) { // Made public as it's a general utility
        return Files.exists(path) && Files.isRegularFile(path);
    }

    /**
     * Checks if the filename of the given path matches common Kustomization file names.
     * (e.g., kustomization.yaml, kustomization.yml, Kustomization)
     * Note: This method only checks the filename, not if the file exists or is a directory.
     * @param path the path to check.
     * @return true if the filename suggests it's a kustomization file.
     */
    public static boolean isKustomizationFileName(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String fileNameStr = fileName.toString();
        return List.of("kustomization.yaml", "kustomization.yml", "Kustomization").contains(fileNameStr);
    }


    /**
     * Checks if the given path likely represents a Kubernetes resource file
     * (has a .yaml, .yml, or .json extension) and is NOT a kustomization file.
     * @param path the path to check.
     * @return true if it's likely a Kubernetes resource file.
     */
    public static boolean isValidKubernetesResource(Path path) {
        Path fileName = path.getFileName();
        if (fileName == null) {
            return false;
        }
        String fileNameStr = fileName.toString();
        // It's a resource if it's NOT a kustomization file by name AND has a valid K8s extension.
        return !isKustomizationFileName(path) && // Uses the local method
                Stream.of(".yaml", ".yml", ".json")
                        .anyMatch(fileNameStr.toLowerCase()::endsWith);
    }

    /**
     * Given a path (which could be a directory or a direct path to a kustomization file),
     * this method finds and returns the actual Kustomization file path
     * (kustomization.yaml, kustomization.yml, or Kustomization in that order of precedence).
     * @param appPath The path representing the application (either a directory or a kustomization file itself).
     * @return The resolved Path to the kustomization file.
     * @throws NotAnAppException if no valid kustomization file can be found.
     */
    public static Path getKustomizationFileFromAppDirectory(Path appPath) throws NotAnAppException {
        logger.debug("Finding kustomization file for app directory/path: {}", appPath);

        // If the provided path itself is a kustomization file (by name) and it exists as a file
        if (isKustomizationFileName(appPath)) {
            if (isFile(appPath)) { // Check if it actually exists as a file
                logger.trace("Provided path '{}' is an existing kustomization file.", appPath);
                return appPath;
            } else {
                // If name matches but it's not a file (e.g., doesn't exist, or is a dir),
                // then we should treat appPath as a directory and search within it.
                logger.trace("Provided path '{}' looks like a kustomization file by name but is not an existing file. Assuming it's a directory to check within.", appPath);
            }
        }

        // If appPath is a directory (or treated as one), look for kustomization files inside
        Path[] potentialKustomizationFiles = {
                appPath.resolve("kustomization.yaml"),
                appPath.resolve("kustomization.yml"),
                appPath.resolve("Kustomization")
        };

        for (Path potentialFile : potentialKustomizationFiles) {
            if (isFile(potentialFile)) {
                logger.trace("Found kustomization file '{}' in app directory '{}'", potentialFile.getFileName(), appPath);
                return potentialFile;
            }
        }

        logger.warn("No valid kustomization file found for app path: {}", appPath);
        throw new NotAnAppException(appPath);
    }
}
