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

import dev.zucca_ops.kustomtrace.exceptions.InvalidReferenceException;
import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Provides various strategies ({@link ReferenceExtractor} instances) for extracting
 * and resolving file path references from different fields within Kustomization files.
 * <p>
 * Each factory method returns a functional interface that takes the reference value
 * (e.g., a string or a list from the Kustomization map) and the base directory
 * of the current Kustomization file, and returns a Stream of resolved {@link Path}s.
 */
public class ReferenceExtractors {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceExtractors.class);

    /**
     * Helper method to check if a path is a valid file, with logging.
     * Primarily used by {@link #configMapGeneratorFiles()}.
     * @param path The path to check.
     * @return True if the path represents an existing, regular file.
     */
    private static boolean isValidFile(Path path) {
        if (KustomizeFileUtil.isFile(path)) {
            return true;
        }
        logger.warn("Invalid reference: path is not a valid file: {}", path);
        return false;
    }

    /**
     * Helper method to check if a path does NOT represent a Kustomization file by name, with logging.
     * Primarily used by {@link #configMapGeneratorFiles()}.
     * @param path The path to check.
     * @return True if the path is NOT a Kustomization file by name.
     */
    private static boolean isNotKustomizationFile(Path path) {
        if (KustomizeFileUtil.isKustomizationFileName(path)) {
            logger.warn("Invalid reference: path points to a kustomization file, which is not expected here: {}", path);
            return false;
        }
        return true;
    }

    /**
     * Validates that the provided reference value is a non-null, single-line string.
     * Used to ensure path strings are not accidentally multi-line.
     * @param referenceValue The object to validate.
     * @return The validated string value.
     * @throws InvalidReferenceException if the value is not a string or contains newlines.
     */
    private static String validateNonMultilineString(Object referenceValue) throws InvalidReferenceException {
        if (!(referenceValue instanceof String valueString)) {
            logger.warn("Invalid reference value type: expected String, got {}. Value: {}",
                    (referenceValue == null ? "null" : referenceValue.getClass().getName()), referenceValue);
            throw new InvalidReferenceException("Non-string value found for reference.");
        }

        if (valueString.contains("\n")) {
            logger.warn("Invalid reference value: multiline string found: '{}'", valueString);
            throw new InvalidReferenceException("Multiline value found in reference.");
        }
        return valueString;
    }

    /**
     * Validates if a given path points to an existing, regular Kubernetes resource file.
     * It checks if the file is a valid Kubernetes resource type (by extension, not a Kustomization file)
     * and if it actually exists as a regular file.
     * @param path The path to validate.
     * @return The validated path if it's a valid and existing resource file.
     * @throws InvalidReferenceException if the path is not a valid Kubernetes resource or does not exist as a file.
     */
    private static Path validateKubernetesResource(Path path) throws InvalidReferenceException {
        if (!KustomizeFileUtil.isValidKubernetesResource(path)) {
            logger.warn("Path is not a valid Kubernetes resource name/type: {}", path);
            throw new InvalidReferenceException("Not a valid Kubernetes resource file name or type.", path);
        }
        // isValidKubernetesResource only checks name patterns. Now check existence.
        if (!KustomizeFileUtil.isFile(path)) {
            logger.warn("Referenced Kubernetes resource file does not exist or is not a regular file: {}", path);
            throw new InvalidReferenceException("Non-existing or non-regular file referenced as a Kubernetes resource.", path, true);
        }
        logger.trace("Found valid Kubernetes resource file reference: {}", path);
        return path;
    }

    /**
     * Returns a {@link ReferenceExtractor} for resolving references to other Kustomization files
     * (e.g., in 'bases' or 'resources' fields that point to kustomizations).
     * It uses {@link KustomizeFileUtil#getKustomizationFileFromAppDirectory(Path)} to resolve
     * the reference to a canonical kustomization file path.
     */
    public static ReferenceExtractor kustomizationFile() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();

            if (path.equals(baseDir.normalize())) {
                logger.warn("Kustomization self-reference detected and disallowed for value '{}' in baseDir '{}'", referenceValue, baseDir);
                throw new InvalidReferenceException("Self reference to kustomization directory not allowed: " + referenceValue, path, true);
            }

            try {
                return Stream.of(KustomizeFileUtil.getKustomizationFileFromAppDirectory(path));
            } catch (NotAnAppException e) {
                logger.warn("Failed to resolve kustomization reference '{}' from baseDir '{}': {}", referenceValue, baseDir, e.getMessage());
                throw new InvalidReferenceException("Invalid directory or kustomization file referenced: " + path, path, e);
            }
        };
    }

    /**
     * Returns a {@link ReferenceExtractor} for resolving references that can be either a direct
     * Kubernetes resource file or a directory containing multiple Kubernetes resource files.
     * <p>
     * If the path is a directory, it streams all valid Kubernetes resource files within that directory.
     * If the path is a file, it validates it as a Kubernetes resource file.
     */
    public static ReferenceExtractor resourceOrDirectory() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();

            if (Files.isDirectory(path)) {
                logger.trace("Path '{}' is a directory. Listing valid Kubernetes resources within.", path);

                String[] directoryContents = new File(path.toAbsolutePath().toString()).list();

                if (directoryContents == null) {
                    logger.error("Could not list directory contents for path (may not be a directory or I/O error occurred): {}", path);
                    throw new InvalidReferenceException("Could not list directory contents (not a directory or I/O error).", path);
                }
                if (directoryContents.length == 0) {
                    logger.warn("Directory referenced by '{}' is empty. No resources to stream.", path);
                    // Kustomize allows empty directories in resources, this means no resources from this path.
                    return Stream.empty();
                }

                return Arrays.stream(directoryContents)
                        .map(path::resolve)
                        .filter(KustomizeFileUtil::isValidKubernetesResource)
                        .filter(KustomizeFileUtil::isFile)
                        .peek(resolvedPath -> logger.trace("Found valid resource file in directory: {}", resolvedPath));
            }

            // If not a directory, treat as a single resource file
            return Stream.of(validateKubernetesResource(path));
        };
    }

    /**
     * Returns a {@link ReferenceExtractor} specifically for resolving references that
     * must be direct Kubernetes resource files (not directories).
     */
    public static ReferenceExtractor resource() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();
            return Stream.of(validateKubernetesResource(path));
        };
    }

    /**
     * Returns a {@link ReferenceExtractor} for resolving a file path found within an inline
     * map structure, typically used in Kustomize patches (e.g., the 'path' field in a patch
     * definition that points to a YAML fragment file).
     *
     * @param pathField The key within the referenceValue map (which represents the patch data)
     * that holds the string value of the path to be resolved.
     * @return A stream containing the resolved {@link Path} to the Kubernetes resource,
     * or an empty stream if the pathField is not found or the referenceValue is not a map.
     */
    public static ReferenceExtractor inlinePathValue(String pathField) {
        return (referenceValue, baseDir) -> {
            logger.debug("Applying inlinePathValue extractor for field '{}', baseDir: {}", pathField, baseDir);

            if (referenceValue instanceof Map) {
                Map<String, Object> valueMap = ((Map<String, Object>) referenceValue);
                Object pathValue = valueMap.get(pathField);
                if (pathValue == null) {
                    logger.warn("Path field '{}' not found in inline map: {}", pathField, valueMap);
                    return Stream.empty();
                }
                String valueStr = validateNonMultilineString(pathValue);
                Path path = baseDir.resolve(valueStr).normalize();
                return Stream.of(validateKubernetesResource(path));
            }

            logger.warn("Invalid reference value for inlinePathValue: expected Map, got {}. Value: {}",
                    (referenceValue == null ? "null" : referenceValue.getClass().getName()), referenceValue);
            return Stream.empty();
        };
    }

    /**
     * Returns a {@link ReferenceExtractor} for extracting file paths from the 'envs' and 'files'
     * fields of a configMapGenerator or secretGenerator.
     * Handles parsing of 'key=value' format in the 'files' list.
     */
    public static ReferenceExtractor configMapGeneratorFiles() {
        return (referenceValue, baseDir) -> {
            logger.debug("Applying configMapGeneratorFiles extractor, baseDir: {}", baseDir);

            if (referenceValue instanceof Map) {
                Map<String, Object> valueMap = ((Map<String, Object>) referenceValue);
                Stream<String> envsFilePaths = Stream.empty();
                if (valueMap.get("envs") instanceof List) {
                    envsFilePaths = ((List<?>) valueMap.get("envs")).stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast);
                } else if (valueMap.containsKey("envs")) {
                    logger.warn("configMapGenerator 'envs' field is not a List in: {}", valueMap);
                }


                Stream<String> filesKeyPaths = Stream.empty();
                if (valueMap.get("files") instanceof List) {
                    filesKeyPaths = ((List<?>) valueMap.get("files")).stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .map(f -> f.contains("=") ? f.substring(f.indexOf("=") + 1).trim() : f.trim());
                } else if (valueMap.containsKey("files")) {
                    logger.warn("configMapGenerator 'files' field is not a List in: {}", valueMap);
                }


                return Stream.concat(envsFilePaths, filesKeyPaths)
                        .filter(s -> !s.isEmpty())
                        .map(f -> baseDir.resolve(f).normalize())
                        .filter(ReferenceExtractors::isValidFile)
                        .filter(ReferenceExtractors::isNotKustomizationFile);
            }
            logger.warn("Invalid reference value for configMapGeneratorFiles: expected Map, got {}. Value: {}",
                    (referenceValue == null ? "null" : referenceValue.getClass().getName()), referenceValue);
            return Stream.empty();
        };
    }
}