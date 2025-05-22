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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ReferenceType {

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

    public String getYamlKey() {
        return yamlKey;
    }

    public Stream<Path> extract(Object yamlValue, Path baseDir) {
        logger.debug("Extracting references for type '{}' with baseDir: {}", this, baseDir);

        try {
            return extractor.extract(yamlValue, baseDir);
        } catch (InvalidReferenceException e) {
            String errorMessage = "Extraction failed for path '%s' and type '%s': %s: %s".formatted(baseDir, this, e.getMessage(), e.getPath().getFileName());
            if (e.isError()) {
                logger.error(errorMessage);
            } else {
                logger.warn(errorMessage);
            }
            return Stream.empty();
        }
    }

    public Stream<Object> getRawReferences(Object yamlValue) {
        logger.debug("Extracting references for type '{}'", this);
        Object value = ((Map<String, Object>) yamlValue).get(this.yamlKey);
        if (!(value instanceof List<?> references)) {
            logger.trace("Input value is not a List, returning empty stream.");
            return Stream.empty();
        }

        return (Stream<Object>) references.stream();
    }

    // Static lookup map for YAML key → enum
    private static final Map<String, ReferenceType> byKey = Arrays.stream(values())
            .collect(Collectors.toMap(ReferenceType::getYamlKey, Function.identity()));

    public static ReferenceType fromYamlKey(String key) {
        logger.debug("Looking up ReferenceType for YAML key: '{}'", key);
        ReferenceType type = byKey.get(key);
        if (type != null) {
            logger.trace("Found ReferenceType '{}' for key '{}'.", type, key);
        } else {
            logger.trace("No ReferenceType found for key '{}'.", key);
        }
        return type;
    }

    public static Boolean isReference(String key) {
        logger.debug("Checking if YAML key '{}' corresponds to a ReferenceType.", key);
        boolean isRef = byKey.containsKey(key);
        logger.trace("Key '{}' is a reference type: {}", key, isRef);
        return isRef;
    }
}