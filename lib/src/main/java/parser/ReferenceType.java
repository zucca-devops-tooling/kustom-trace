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

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ReferenceType {

    RESOURCE("resources", ReferenceExtractors.resource(), ReferenceValidators.mustBeKubernetesResource()),
    BASE("bases", ReferenceExtractors.kustomizationFile(), ReferenceValidators.mustBeDirectoryOrKustomizationFile()),
    COMPONENT("components", ReferenceExtractors.kustomizationFile(), ReferenceValidators.mustBeDirectoryOrKustomizationFile()),
    PATCH("patches", ReferenceExtractors.inlinePathValue("path"), ReferenceValidators.mustBeFile()),
    PATCH_MERGE("patchesStrategicMerge", ReferenceExtractors.resource(), ReferenceValidators.mustBeKubernetesResource()),
    CONFIG_MAP("configMapGenerator", ReferenceExtractors.configMapGeneratorFiles(), ReferenceValidators.mustBeFile());

    private final String yamlKey;
    private final ReferenceExtractor extractor;
    private final ReferenceValidator validator;
    private static final Logger logger = LoggerFactory.getLogger(ReferenceType.class);

    ReferenceType(String yamlKey, ReferenceExtractor extractor, ReferenceValidator validator) {
        this.yamlKey = yamlKey;
        this.extractor = extractor;
        this.validator = validator;
    }

    public String getYamlKey() {
        return yamlKey;
    }

    public Stream<Path> extract(Object yamlValue, Path baseDir) {
        logger.debug("Extracting references for type '{}' with baseDir: {}", this, baseDir);
        return extractor.extract(yamlValue, baseDir)
                .peek(path -> logger.trace("Extracted path '{}' for type '{}'.", path, this));
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

    public boolean validate(Path path) {
        logger.debug("Validating path '{}' for type '{}'.", path, this);
        try {
            validator.validate(path);
            logger.trace("Path '{}' is valid for type '{}'.", path, this);
            return true;
        } catch (InvalidReferenceException e) {
            logger.warn("Validation failed for path '{}' and type '{}': {}", path, this, e.getMessage());
            return false;
        }
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