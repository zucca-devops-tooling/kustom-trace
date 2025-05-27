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

import static dev.zucca_ops.kustomtrace.parser.KustomizeFileUtil.isDirectory;

import dev.zucca_ops.kustomtrace.exceptions.InvalidReferenceException;
import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            logger.warn(
                    "Invalid reference: path points to a kustomization file, which is not expected here: {}",
                    path);
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
    private static String validateNonMultilineString(Object referenceValue)
            throws InvalidReferenceException {
        if (!(referenceValue instanceof String valueString)) {
            throw new InvalidReferenceException("Non-string value found for reference.");
        }

        if (valueString.contains("\n")) {
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
            throw new InvalidReferenceException("Not a valid Kubernetes resource", path);
        }
        // isValidKubernetesResource only checks name patterns. Now check existence.
        if (!KustomizeFileUtil.isFile(path)) {
            throw new InvalidReferenceException(
                    "Non-existing or non-regular file referenced as a Kubernetes resource.",
                    path,
                    true);
        }
        logger.trace("Found valid Kubernetes resource file reference: {}", path);
        return path;
    }

    /**
     * Helper to resolve a path to a canonical Kustomization file.
     * @throws InvalidReferenceException if path doesn't lead to a valid Kustomization.
     */
    private static Path extractKustomization(Path path, Path baseDir)
            throws InvalidReferenceException {
        if (path.equals(baseDir.normalize())) {
            throw new InvalidReferenceException(
                    "Self reference to kustomization directory not allowed: " + path.getFileName(),
                    path,
                    true);
        }

        try {
            return KustomizeFileUtil.getKustomizationFileFromAppDirectory(path);
        } catch (NotAnAppException e) {
            throw new InvalidReferenceException(
                    "Expected directory with Kustomization inside: " + path, path, e);
        }
    }

    /**
     * Returns a {@link ReferenceExtractor} for Kustomize fields (e.g., 'bases', 'components')
     * that must reference directories.
     * <p>
     * The returned extractor expects the resolved path to be a directory and will then
     * attempt to find a kustomization definition file (e.g., "kustomization.yaml") within it.
     *
     * @return A {@link ReferenceExtractor}. Its {@code extract} method returns a stream with a
     * single {@link Path} to the resolved kustomization definition file. The {@code extract}
     * method may throw an {@link InvalidReferenceException} if the input path is not a directory,
     * if no valid kustomization definition is found, or if a self-reference is detected.
     */
    public static ReferenceExtractor directory() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();

            if (!isDirectory(path)) {
                throw new InvalidReferenceException("Expected a directory", path, true);
            }

            return Stream.of(extractKustomization(path, baseDir));
        };
    }

    /**
     * Extractor for Kustomize 'resources' field entries.
     * <p>
     * Attempts to resolve the path as a Kustomization target first.
     * If that fails, it treats the path as a direct Kubernetes resource file.
     *
     * @return A stream containing the resolved {@link Path} to either a kustomization
     * definition file or a validated Kubernetes resource file.
     * method may throw an {@link InvalidReferenceException} if the input path is not a directory,
     * target nor a valid, existing Kubernetes resource file.
     */
    public static ReferenceExtractor resourceOrDirectory() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();

            if (isDirectory(path)) {
                logger.trace("Path '{}' is a directory. finding kustomization within.", path);
                return Stream.of(extractKustomization(path, baseDir));
            }

            logger.trace("Path '{}' is a file.", path);
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
            logger.debug(
                    "Applying inlinePathValue extractor for field '{}', baseDir: {}",
                    pathField,
                    baseDir);

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
                    envsFilePaths =
                            ((List<?>) valueMap.get("envs"))
                                    .stream()
                                            .filter(String.class::isInstance)
                                            .map(String.class::cast);
                } else if (valueMap.containsKey("envs")) {
                    logger.warn("configMapGenerator 'envs' field is not a List in: {}", valueMap);
                }

                Stream<String> filesKeyPaths = Stream.empty();
                if (valueMap.get("files") instanceof List) {
                    filesKeyPaths =
                            ((List<?>) valueMap.get("files"))
                                    .stream()
                                            .filter(String.class::isInstance)
                                            .map(String.class::cast)
                                            .map(
                                                    f ->
                                                            f.contains("=")
                                                                    ? f.substring(
                                                                                    f.indexOf("=")
                                                                                            + 1)
                                                                            .trim()
                                                                    : f.trim());
                } else if (valueMap.containsKey("files")) {
                    logger.warn("configMapGenerator 'files' field is not a List in: {}", valueMap);
                }

                return Stream.concat(envsFilePaths, filesKeyPaths)
                        .filter(s -> !s.isEmpty())
                        .map(f -> baseDir.resolve(f).normalize())
                        .filter(ReferenceExtractors::isValidFile)
                        .filter(ReferenceExtractors::isNotKustomizationFile);
            }
            logger.warn(
                    "Invalid reference value for configMapGeneratorFiles: expected Map, got {}. Value: {}",
                    (referenceValue == null ? "null" : referenceValue.getClass().getName()),
                    referenceValue);
            return Stream.empty();
        };
    }
}
