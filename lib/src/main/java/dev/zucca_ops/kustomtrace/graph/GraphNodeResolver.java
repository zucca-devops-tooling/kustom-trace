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
package dev.zucca_ops.kustomtrace.graph;

import dev.zucca_ops.kustomtrace.exceptions.InvalidContentException;
import dev.zucca_ops.kustomtrace.model.KustomFile;
import dev.zucca_ops.kustomtrace.model.KustomResource;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import dev.zucca_ops.kustomtrace.parser.KustomizeFileUtil;
import dev.zucca_ops.kustomtrace.parser.YamlParser;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves file paths into Kustomize model objects like {@link KustomFile},
 * {@link KustomResource}, and {@link Kustomization} by parsing their content.
 * Intended for internal use within graph-building processes.
 */
public class GraphNodeResolver {
    private static final Logger logger = LoggerFactory.getLogger(GraphNodeResolver.class);

    /**
     * Resolves a file path into a {@link KustomFile}. If the file is parseable
     * (e.g., .yaml, .yml, .json), its content is parsed into {@link KustomResource}s.
     *
     * @param path The {@link Path} to the file.
     * @return A {@link KustomFile} representing the file.
     * @throws InvalidContentException If a parseable file contains non-map document roots.
     * @throws FileNotFoundException If the file cannot be found or read.
     */
    static KustomFile resolveKustomFile(Path path)
            throws InvalidContentException, FileNotFoundException {
        logger.debug("Resolving KustomFile: {}", path);
        KustomFile file = new KustomFile(path);

        if (KustomizeFileUtil.isValidKubernetesResource(path)) {
            logger.debug(
                    "Path identified as a valid Kubernetes resource, attempting to parse: {}",
                    path);
            YamlParser.parseFile(path).stream()
                    .map(
                            GraphNodeResolver
                                    ::resolveResource) // Converts each Map doc to a KustomResource
                    .forEach(
                            resource -> {
                                file.addResource(resource);
                                resource.setFile(file);
                            });
            logger.debug("Finished processing resources in: {}", path);
        } else {
            logger.debug(
                    "Skipping parsing for path (not a standard Kubernetes resource file or is a Kustomization file by name): {}",
                    path);
        }
        return file;
    }

    /**
     * Converts a parsed YAML document map into a {@link KustomResource}.
     * Extracts 'kind' and 'metadata.name' if present and correctly typed.
     *
     * @param document A Map representing a single parsed YAML document.
     * @return A {@link KustomResource}.
     */
    static KustomResource resolveResource(Map<String, Object> document) {
        logger.trace("Resolving KustomResource from document");
        KustomResource resource = new KustomResource();

        resource.setKind(YamlParser.getKind(document));

        Object metadataObj = document.get("metadata");
        if (metadataObj instanceof Map) {
            Map<String, Object> metadata = (Map<String, Object>) metadataObj;
            Object nameObj = metadata.get("name");
            if (nameObj instanceof String) {
                resource.setName((String) nameObj);
            } else {
                if (metadata.containsKey("name")) {
                    logger.debug(
                            "Resource metadata 'name' is present but not a String. Found: {}. Metadata keys: {}",
                            nameObj != null ? nameObj.getClass().getName() : "null",
                            metadata.keySet());
                }
            }
        } else {
            if (document.containsKey("metadata")) {
                logger.debug(
                        "Resource 'metadata' is present but not a Map. Found: {}. Document snippet: {}",
                        metadataObj != null ? metadataObj.getClass().getName() : "null",
                        document.keySet());
            }
        }
        return resource;
    }

    /**
     * Resolves a kustomization file path into a {@link Kustomization} object.
     *
     * @param path The {@link Path} to the kustomization file.
     * @return A {@link Kustomization} representing the parsed content.
     * @throws InvalidContentException If parsing fails or content is invalid (e.g., not a single map document).
     * @throws FileNotFoundException If the file cannot be found or read.
     */
    static Kustomization resolveKustomization(Path path)
            throws InvalidContentException, FileNotFoundException {
        logger.debug("Resolving Kustomization from: {}", path);
        Map<String, Object> fileContent = YamlParser.parseKustomizationFile(path);
        logger.debug("Successfully resolved Kustomization from: {}", path);

        return new Kustomization(path, fileContent);
    }
}
