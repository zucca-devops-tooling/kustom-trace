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

import dev.zucca_ops.kustomtrace.exceptions.InvalidContentException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

/**
 * Utility class for parsing YAML files, particularly Kubernetes manifests and Kustomization files,
 * using the SnakeYAML library.
 */
public class YamlParser {

    private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);

    /**
     * Parses a YAML file, potentially containing multiple YAML documents (separated by "---").
     * Each document is expected to be a YAML map (key-value structure).
     *
     * @param path The {@link Path} to the YAML file to be parsed.
     * @return A list of maps, where each map represents a YAML document in the file.
     * Returns an empty list if the file is empty or contains only null documents.
     * @throws FileNotFoundException If the file specified by the path does not exist,
     * is a directory, or cannot be read.
     * @throws InvalidContentException If any document in the YAML file is not a map
     * (e.g., it's a list or a scalar at the root).
     */
    public static List<Map<String, Object>> parseFile(Path path)
            throws FileNotFoundException, InvalidContentException {
        logger.debug("Starting to parse YAML file: {}", path);
        Yaml parser = new Yaml();
        List<Map<String, Object>> documents = new ArrayList<>();

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            logger.error("YAML file does not exist or is not a regular file: {}", path);
            throw new FileNotFoundException("File not found or is not a regular file: " + path);
        }

        try (InputStream inputStream =
                new FileInputStream(
                        path.toAbsolutePath().toFile())) { // Using toFile() for FileInputStream
            Iterable<Object> parsedDocs = parser.loadAll(inputStream);
            int documentCount = 0;

            for (Object doc : parsedDocs) {
                if (Objects.isNull(doc)) {
                    logger.warn("Found and skipped a null YAML document in: {}", path);
                    continue;
                }

                if (doc instanceof Map<?, ?> map) {
                    Map<String, Object> typedMap = (Map<String, Object>) map;
                    documents.add(typedMap);
                    documentCount++;
                } else {
                    logger.error(
                            "Document in {} is not a Map (key-value structure), actual type: {}",
                            path,
                            doc.getClass().getName());
                    throw new InvalidContentException(path);
                }
            }

            logger.debug(
                    "Successfully parsed {} YAML map document(s) from: {}", documentCount, path);
            return documents;
        } catch (
                FileNotFoundException
                        fnfe) { // Catch specific first if FileInputStream throws it due to
            // concurrent deletion/permission change
            logger.error("Error opening YAML file (FileNotFoundException): {}", path, fnfe);
            throw fnfe;
        } catch (IOException e) {
            logger.error("Error reading YAML file: {}", path, e);
            throw new FileNotFoundException(
                    "Could not read YAML file: " + path + ". Reason: " + e.getMessage());
        } catch (Exception e) { // Catch potential SnakeYAML runtime parsing exceptions (e.g.,
            // YAMLException)
            logger.error("Error parsing YAML content in file: {}", path, e);
            throw new InvalidContentException(path, e);
        }
    }

    /**
     * Parses a Kustomization file, which is expected to be a single YAML document
     * representing a map.
     *
     * @param path The {@link Path} to the Kustomization file.
     * @return A map representing the parsed Kustomization content.
     * @throws FileNotFoundException If the file does not exist or cannot be read.
     * @throws InvalidContentException If the file is empty, contains multiple YAML documents,
     * or if the document is not a map.
     */
    public static Map<String, Object> parseKustomizationFile(Path path)
            throws InvalidContentException, FileNotFoundException {
        logger.debug("Attempting to parse kustomization file: {}", path);

        // parseFile will handle FileNotFoundException and issues with non-map documents.
        List<Map<String, Object>> fileContent = YamlParser.parseFile(path);

        if (fileContent.isEmpty()) {
            // This case means parseFile returned an empty list (e.g., file was empty,
            // or only contained 'null' documents like "---").
            logger.warn(
                    "Kustomization file at {} is effectively empty or contains no valid YAML map documents.",
                    path);
            throw new InvalidContentException(path);
        }

        if (fileContent.size() > 1) {
            logger.error(
                    "Kustomization file at {} contains {} documents, but exactly one was expected.",
                    path,
                    fileContent.size());
            throw new InvalidContentException(path);
        }

        return fileContent.get(0);
    }
}
