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
import java.util.Objects;
import java.util.stream.Stream;

public class ReferenceExtractors {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceExtractors.class);

    private static boolean isValidFile(Path path) {
        // this method is just to apply logging
        if (KustomizeFileUtil.isFile(path)) {
            return true;
        }
        logger.warn("Invalid reference to non file " + path);
        return false;
    }

    private static boolean isNotKustomizationFile(Path path) {
        // this method is just to apply logging
        if (KustomizeFileUtil.isKustomizationFileName(path)) {
            logger.warn("Invalid reference to kustomization file " + path);
            return false;
        }
        return true;
    }

    private static String validateNonMultilineString(Object referenceValue) throws InvalidReferenceException {
        Path invalid = Path.of("invalid");
        if (!(referenceValue instanceof String)) {
            throw new InvalidReferenceException("Non string value found", invalid);
        }

        String value = referenceValue.toString();

        if (value.contains("\n")) {
            throw new InvalidReferenceException("Multiline value found", invalid);
        }

        return value;
    }

    private static Path validateKubernetesResource(Path path) throws InvalidReferenceException {
        if (KustomizeFileUtil.isValidKubernetesResource(path)) {
            if (!KustomizeFileUtil.isFile(path)) {
                throw new InvalidReferenceException("Non-Existing file referenced", path, true);
            }

            logger.trace("Found file reference: {}", path);
            return path;
        }

        throw new InvalidReferenceException("Not a valid kubernetes resource", path);
    }

    public static ReferenceExtractor kustomizationFile() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();

            if (path.equals(baseDir)) {
                throw new InvalidReferenceException("Self reference " + referenceValue, path, true);
            }

            try {
                return Stream.of(KustomizeFileUtil.getKustomizationFileFromAppDirectory(path));
            } catch (NotAnAppException e) {
                throw new InvalidReferenceException("Invalid directory or kustomization file", path);
            }
        };
    }

    public static ReferenceExtractor resourceOrDirectory() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();

            if (Files.isDirectory(path)) {
                logger.trace("Path '{}' is a valid directory containing a kustomization file.", path);

                String[] directory = new File(path.toAbsolutePath().toString()).list();

                if (directory == null || directory.length == 0) {
                    throw new InvalidReferenceException("Empty directory found", path);
                }

                return Arrays.stream(Objects.requireNonNull(directory))
                        .map(path::resolve)
                        .filter(KustomizeFileUtil::isValidKubernetesResource)
                        .peek(resolvedPath -> logger.trace("Found file in directory: {}", resolvedPath));
            }

            return Stream.of(validateKubernetesResource(path));
        };
    }

    public static ReferenceExtractor resource() {
        return (referenceValue, baseDir) -> {
            Path path = baseDir.resolve(validateNonMultilineString(referenceValue)).normalize();

            return Stream.of(validateKubernetesResource(path));
        };
    }

    public static ReferenceExtractor inlinePathValue(String pathField) {
        return (referenceValue, baseDir) -> {
            logger.debug("Applying inlinePathList reference extractor with pathField '{}' and baseDir: {}", pathField, baseDir);

            if (referenceValue instanceof Map) {
                Map<String, Object> valueMap = ((Map<String, Object>) referenceValue);
                String value = validateNonMultilineString(valueMap.get(pathField));

                Path path = baseDir.resolve(value).normalize();

                return Stream.of(validateKubernetesResource(path));
            }

            logger.trace("Invalid value");
            return Stream.empty();
        };
    }

    public static ReferenceExtractor configMapGeneratorFiles() {

        return (referenceValue, baseDir) -> {
            logger.debug("Applying configMapGeneratorFiles reference extractor with baseDir: {}", baseDir);

            if (referenceValue instanceof Map) {
                Map<String, Object> valueMap = ((Map<String, Object>) referenceValue);
                Stream<String> envs = valueMap.containsKey("envs")
                        ? ((List<String>) valueMap.get("envs")).stream()
                        : Stream.empty();

                Stream<String> files = valueMap.containsKey("files")
                        ? ((List<String>) valueMap.get("files")).stream()
                        .map(f -> f.contains("=") ? f.substring(f.indexOf("=") + 1) : f)
                        : Stream.empty();

                return Stream.concat(envs, files)
                        .map(f -> baseDir.resolve(f).normalize())
                        .filter(ReferenceExtractors::isValidFile)
                        .filter(ReferenceExtractors::isNotKustomizationFile);
            }

            return Stream.empty();
        };
    }
}