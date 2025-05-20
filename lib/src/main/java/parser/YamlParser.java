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

import exceptions.InvalidContentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class YamlParser {

    private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);

    public static List<Map<String, Object>> parseFile(Path path) throws FileNotFoundException, InvalidContentException {
        logger.debug("Starting to parse YAML file: {}", path.toAbsolutePath());
        Yaml parser = new Yaml();
        List<Map<String, Object>> documents = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(path.toAbsolutePath().toString())) {
            Iterable<Object> parsed = parser.loadAll(inputStream);
            int documentCount = 0;

            for (Object doc : parsed) {
                if (Objects.isNull(doc)) {
                    logger.warn("Found a null YAML document in: {}", path);
                    continue;
                }

                if (doc instanceof Map<?, ?> map) {
                    //noinspection unchecked
                    documents.add((Map<String, Object>) map);
                    documentCount++;
                } else {
                    throw new InvalidContentException(path);
                }
            }

            logger.debug("Processed {} documents from: {}", documentCount, path);
            return documents;
        } catch (IOException e) {
            logger.error("Error reading YAML file: {}", path, e);
            throw new FileNotFoundException("Could not read YAML file: " + path);
        }
    }

    public static Map<String, Object> parseKustomizationFile(Path path) throws InvalidContentException, FileNotFoundException {
        logger.debug("Attempting to parse kustomization file: {}", path);

        List<Map<String, Object>> fileContent = YamlParser.parseFile(path);

        if (fileContent == null || fileContent.isEmpty()) {
            logger.warn("kustomization.yaml at {} is empty or contains no documents.", path);
            return null;
        }

        if (fileContent.size() > 1) {
            logger.error("kustomization.yaml at {} contains {} documents, expected one.", path, fileContent.size());
            return null;
        }

        return fileContent.get(0);
    }

    public static boolean isValidKustomizationFile(Path path) {
        return Stream.of("kustomization.yaml", "kustomization.yml")
                .anyMatch(path.getFileName().toString()::endsWith);
    }

    public static boolean isValidKubernetesResource(Path path) {
        return !isValidKustomizationFile(path) && Stream.of(".yaml", ".yml", ".json")
                .anyMatch(path.getFileName().toString().toLowerCase()::endsWith);
    }
}