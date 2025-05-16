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
package graph;

import exceptions.InvalidContentException;
import model.KustomFile;
import model.KustomResource;
import model.Kustomization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.YamlParser;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class GraphNodeResolver {
    private static final Logger logger = LoggerFactory.getLogger(GraphNodeResolver.class);

    static KustomFile resolveKustomFile(Path path) {
        logger.debug("Resolving KustomFile: {}", path);
        Stream<String> parseableExtensions = Stream.of(".yml", ".yaml", ".json");

        KustomFile file = new KustomFile(path);
        String fileName = path.getFileName().toString();

        if (parseableExtensions.anyMatch(fileName::endsWith)) {
            logger.debug("Parsing parseable file: {}", path);
            try {
                YamlParser.parseFile(path)
                        .stream()
                        .map(GraphNodeResolver::resolveResource)
                        .forEach(resource -> {
                            file.addResource(resource);
                            resource.setFile(file);
                        });
                logger.debug("Finished processing resources in: {}", path);
            } catch (FileNotFoundException e) {
                logger.error("Could not find file: {}", path, e);
            }
        } else {
            logger.debug("Skipping non-parseable file: {}", path);
        }

        return file;
    }

    static KustomResource resolveResource(Map<String, Object> document) {
        logger.trace("Resolving KustomResource from document: {}", document);
        KustomResource resource = new KustomResource();

        if (document.containsKey("kind")) {
            resource.setKind((String) document.get("kind"));
        } else {
            logger.debug("Resource does not contain 'kind': {}", document);
        }

        if (document.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
            if (metadata.containsKey("name")) {
                resource.setName((String) metadata.get("name"));
            } else {
                logger.debug("Resource metadata does not contain 'name': {}", metadata);
            }
        } else {
            logger.debug("Resource does not contain 'metadata': {}", document);
        }

        return resource;
    }

    static Kustomization resolveKustomization(Path path) throws InvalidContentException {
        logger.debug("Resolving Kustomization from: {}", path);
        Map<String, Object> fileContent = YamlParser.parseKustomizationFile(path);

        if (fileContent == null) {
            logger.debug("YamlParser returned null when trying to parse kustomization file: {}", path);
            throw new InvalidContentException(path);
        }

        logger.debug("Successfully resolved Kustomization from: {}", path);
        return new Kustomization(path, fileContent);
    }
}