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
package parser;

import exceptions.InvalidReferenceException;
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

    public static ReferenceExtractor kustomizationFile() {
        return (referenceValue, baseDir) -> {
            if (referenceValue instanceof String) {
                String value = referenceValue.toString();

                if (value.contains("\n")) {
                    logger.trace("Multiline value");
                    return Stream.empty();
                }

                Path path = baseDir.resolve(value).normalize();

                if (Files.isDirectory(path)) {
                    Path kustomizationYaml = path.resolve("kustomization.yaml");
                    Path kustomizationYml = path.resolve("kustomization.yml");
                    logger.trace("Path '{}' is a valid directory containing a kustomization file.", path);

                    return Stream.of(Files.exists(kustomizationYaml) ? kustomizationYaml : kustomizationYml);
                }

                if (YamlParser.isValidKustomizationFile(path)) {
                    logger.trace("Found kustomization yaml: {}", path);
                    return Stream.of(path);
                }
            }

            logger.trace("Non string value");
            return Stream.empty();
        };
    }

    public static ReferenceExtractor resource() {
        return (referenceValue, baseDir) -> {
            if (referenceValue instanceof String) {
                String value = referenceValue.toString();

                if (value.contains("\n")) {
                    logger.trace("Multiline value");
                    return Stream.empty();
                }

                Path path = baseDir.resolve(value).normalize();

                if (Files.isDirectory(path)) {
                    logger.trace("Path '{}' is a valid directory containing a kustomization file.", path);

                    File directory = new File(path.toAbsolutePath().toString());
                    return Arrays.stream(Objects.requireNonNull(directory.list()))
                            .map(path::resolve)
                            .peek(resolvedPath -> logger.trace("Found file in directory: {}", resolvedPath));
                }

                if (!YamlParser.isValidKustomizationFile(path)) {
                    logger.trace("Found file reference: {}", path);
                    return Stream.of(path);
                }
            }

            logger.trace("Non string value");
            return Stream.empty();
        };
    }

    public static ReferenceExtractor inlinePathValue(String pathField) {
        return (referenceValue, baseDir) -> {
            logger.debug("Applying inlinePathList reference extractor with pathField '{}' and baseDir: {}", pathField, baseDir);

            if (referenceValue instanceof Map) {
                Map<String, Object> valueMap = ((Map<String, Object>) referenceValue);
                Object value = valueMap.get(pathField);

                if (value != null) {
                    String path = value.toString();
                    logger.trace("Found inline path reference: {}", path);
                    return Stream.of(baseDir.resolve(path).normalize());
                }
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
                        .peek(env -> logger.trace("Found env reference: {}", env))
                        : Stream.empty();

                Stream<String> files = valueMap.containsKey("files")
                        ? ((List<String>) valueMap.get("files")).stream()
                        .map(f -> f.contains("=") ? f.substring(f.indexOf("=") + 1) : f)
                        .peek(file -> logger.trace("Found file reference: {}", file))
                        : Stream.empty();

                return Stream.concat(envs, files)
                        .map(f -> baseDir.resolve(f).normalize());
            }

            return Stream.empty();
        };
    }
}