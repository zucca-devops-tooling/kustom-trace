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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines the different types of Kustomize fields that can contain references
 * to other resources or Kustomization files.
 * <p>
 * Each enum constant is associated with a specific YAML key found in Kustomization files
 * and a {@link ReferenceExtractor} strategy responsible for parsing the value of that key
 * to extract and resolve file paths.
 */
public enum ReferenceType {

    // Enum constants define the specific reference types, their YAML keys,
    // and the appropriate ReferenceExtractor to use.
    RESOURCE("resources", ReferenceExtractors.resourceOrDirectory()),
    BASE("bases", ReferenceExtractors.kustomizationFile()),
    COMPONENT("components", ReferenceExtractors.kustomizationFile()),
    PATCH("patches", ReferenceExtractors.inlinePathValue("path")),
    PATCH_MERGE("patchesStrategicMerge", ReferenceExtractors.resource()),
    CONFIG_MAP("configMapGenerator", ReferenceExtractors.configMapGeneratorFiles());

    private final String yamlKey;
    private final ReferenceExtractor extractor;
    private static final Logger logger = LoggerFactory.getLogger(ReferenceType.class);

    ReferenceType(String yamlKey, ReferenceExtractor extractor) {
        this.yamlKey = yamlKey;
        this.extractor = extractor;
    }

    /**
     * Gets the YAML key string associated with this reference type
     * (e.g., "resources", "bases").
     * @return The YAML key.
     */
    public String getYamlKey() {
        return yamlKey;
    }

    /**
     * Extracts and resolves file paths from the given YAML value using the
     * {@link ReferenceExtractor} associated with this reference type.
     * Handles {@link InvalidReferenceException} from the extractor by logging
     * and returning an empty stream.
     *
     * @param yamlValue The raw value associated with this reference type's key,
     * obtained from the parsed Kustomization YAML content.
     * @param baseDir   The base directory for resolving relative paths found in the yamlValue.
     * @return A {@link Stream} of resolved {@link Path} objects. Returns an empty
     * stream if extraction fails or no valid references are found.
     */
    public Stream<Path> extract(Object yamlValue, Path baseDir) {
        logger.debug(
                "Extracting references for type '{}' using key '{}' with baseDir: {}",
                this.name(),
                this.yamlKey,
                baseDir);

        try {
            return extractor.extract(yamlValue, baseDir);
        } catch (InvalidReferenceException e) {
            String errorMessage =
                    "Extraction failed for path '%s' and type '%s': %s: %s"
                            .formatted(baseDir, this, e.getMessage(), e.getPath().getFileName());
            if (e.isError()) {
                logger.error(errorMessage);
            } else {
                logger.warn(errorMessage);
            }
            return Stream.empty(); // Gracefully return empty stream on handled extraction issues.
        }
    }

    /**
     * Retrieves raw reference values from a Kustomization content map for this reference type.
     * Expects the value associated with {@link #getYamlKey()} to be a List.
     *
     * @param kustomizationContentMap The parsed content of a Kustomization file.
     * @return A stream of objects from the list, or an empty stream if the key is absent
     * or its value is not a list.
     */
    public Stream<Object> getRawReferences(Map<String, Object> kustomizationContentMap) {
        logger.debug(
                "Getting raw reference values for type '{}' (key: '{}')",
                this.name(),
                this.yamlKey);
        Object value = kustomizationContentMap.get(this.yamlKey);

        if (!(value instanceof List<?> referencesList)) {
            if (value != null) {
                logger.warn(
                        "Expected a List for key '{}', but found {}. Kustomization content: {}",
                        this.yamlKey,
                        value.getClass().getName(),
                        kustomizationContentMap);
            } else {
                logger.trace(
                        "Key '{}' not found or value is null in Kustomization content.",
                        this.yamlKey);
            }
            return Stream.empty();
        }

        return (Stream<Object>)
                referencesList.stream(); // Cast to Stream<Object> for broader compatibility
    }

    // Static lookup map for YAML key → enum, initialized once.
    private static final Map<String, ReferenceType> BY_YAML_KEY =
            Arrays.stream(values())
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    ReferenceType::getYamlKey, Function.identity()));

    /**
     * Retrieves a {@link ReferenceType} enum constant based on its YAML key.
     * @param key The YAML key string (e.g., "resources", "bases").
     * @return The corresponding {@link ReferenceType}, or null if no match is found.
     */
    public static ReferenceType fromYamlKey(String key) {
        logger.debug("Looking up ReferenceType for YAML key: '{}'", key);
        ReferenceType type = BY_YAML_KEY.get(key);
        if (type != null) {
            logger.trace("Found ReferenceType '{}' for key '{}'.", type, key);
        } else {
            logger.trace("No ReferenceType found for key '{}'.", key);
        }
        return type;
    }

    /**
     * Checks if a given YAML key corresponds to a known {@link ReferenceType}.
     * @param key The YAML key string.
     * @return True if the key is a known reference type, false otherwise.
     */
    public static Boolean isReferenceKey(String key) {
        logger.debug("Checking if YAML key '{}' corresponds to a ReferenceType.", key);
        boolean isRef = BY_YAML_KEY.containsKey(key);
        logger.trace("Key '{}' is a reference type: {}", key, isRef);
        return isRef;
    }
}
