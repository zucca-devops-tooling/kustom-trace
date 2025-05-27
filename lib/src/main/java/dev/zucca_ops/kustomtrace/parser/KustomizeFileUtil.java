/*
 * Copyright 2025 GuidoZuccarelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zucca_ops.kustomtrace.parser;

import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods for Kustomize file and path conventions.
 */
public class KustomizeFileUtil {

    private static final Logger logger = LoggerFactory.getLogger(KustomizeFileUtil.class);

    /**
     * Checks if the path exists and is a regular file.
     * @param path The path to check.
     * @return {@code true} if the path exists and is a regular file, {@code false} otherwise.
     */
    public static boolean isFile(Path path) {
        return Files.exists(path) && Files.isRegularFile(path);
    }

    /**
     * Checks if the path exists and is a directory.
     * @param path The path to check.
     * @return {@code true} if the path exists and is a directory, {@code false} otherwise.
     */
    public static boolean isDirectory(Path path) {
        return Files.exists(path) && Files.isDirectory(path);
    }

    /**
     * Checks if the path's filename is a standard Kustomization filename.
     * (kustomization.yaml, kustomization.yml, or Kustomization).
     * @param path The path to check.
     * @return {@code true} if the filename matches a Kustomization name.
     */
    public static boolean isKustomizationFileName(Path path) {
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            return false;
        }
        String fileNameStr = fileNamePath.toString();
        return List.of("kustomization.yaml", "kustomization.yml", "Kustomization")
                .contains(fileNameStr);
    }

    /**
     * Checks if the path likely represents a Kubernetes resource file (by extension)
     * and is not a Kustomization file by name.
     * @param path The path to check.
     * @return {@code true} if it's likely a Kubernetes resource file.
     */
    public static boolean isValidKubernetesResource(Path path) {
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            return false;
        }
        String fileNameStr = fileNamePath.toString();
        return !isKustomizationFileName(path)
                && Stream.of(".yaml", ".yml", ".json")
                        .anyMatch(fileNameStr.toLowerCase()::endsWith);
    }

    /**
     * Resolves an application path (directory or direct kustomization file path)
     * to the actual Kustomization definition file.
     * Searches for "kustomization.yaml", "kustomization.yml", "Kustomization" in order.
     *
     * @param appPath The application path (directory or kustomization file).
     * @return The resolved {@link Path} to the kustomization file.
     * @throws NotAnAppException if no valid kustomization file is found.
     */
    public static Path getKustomizationFileFromAppDirectory(Path appPath) throws NotAnAppException {
        logger.debug("Finding kustomization file for app path: {}", appPath);

        Path[] potentialFiles = {
            appPath.resolve("kustomization.yaml"),
            appPath.resolve("kustomization.yml"),
            appPath.resolve("Kustomization")
        };

        for (Path potentialFile : potentialFiles) {
            if (isFile(potentialFile)) {
                logger.trace(
                        "Found kustomization file '{}' in app directory '{}'",
                        potentialFile.getFileName(),
                        appPath);
                return potentialFile;
            }
        }

        logger.warn("No valid kustomization file found for app path: {}", appPath);
        throw new NotAnAppException(appPath);
    }
}
