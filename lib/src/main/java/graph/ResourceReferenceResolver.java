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

import exceptions.InvalidReferenceException;
import model.GraphNode;
import model.Kustomization;
import model.ResourceReference;
import parser.ReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceReferenceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ResourceReferenceResolver.class);
    KustomGraphBuilder builder;

    public ResourceReferenceResolver(KustomGraphBuilder builder) {
        this.builder = builder;
    }

    Stream<ResourceReference> resolveDependency(ReferenceType type, Path path) {
        logger.debug("Resolving dependency of type '{}' at path: {}", type, path);
        try {
            type.validate(path);
        } catch (InvalidReferenceException e) {
            logger.warn("Invalid reference found for type '{}' at path: {}", type, path, e);
            return Stream.empty();
        }

        if (Files.isDirectory(path)) {
            logger.debug("Path is a directory, resolving directory: {}", path);
            return resolveDirectory(path)
                    .map(node -> new ResourceReference(type, node));
        }

        logger.debug("Path is a file, building KustomFile: {}", path);
        return Stream.of(new ResourceReference(type, builder.buildKustomFile(path)));
    }

    Stream<ResourceReference> resolveDependencies(Kustomization kustomization) {
        logger.debug("Resolving dependencies for Kustomization: {}", kustomization.getPath());
        Map<String, Object> fileContent = kustomization.getContent();

        return fileContent.keySet().stream()
                .filter(ReferenceType::isReference)
                .map(ReferenceType::fromYamlKey)
                .peek(referenceType -> logger.debug("Processing reference type: {}", referenceType))
                .flatMap(referenceType ->
                        referenceType.extract(fileContent, kustomization.getPath().getParent())
                                .peek(dependencyPath -> logger.debug("Extracted dependency path: {}", dependencyPath))
                                .flatMap(dependency -> resolveDependency(referenceType, dependency))
                );
    }

    Stream<GraphNode> resolveDirectory(Path path) {
        logger.debug("Resolving directory: {}", path);
        Path kustomization = path.resolve("kustomization.yaml");
        Stream<? extends GraphNode> nodes;

        if (Files.exists(kustomization)) {
            logger.debug("Found kustomization.yaml, building Kustomization from: {}", kustomization);
            nodes = Stream.of(builder.buildKustomization(kustomization));
        } else {
            logger.debug("kustomization.yaml not found, listing files in directory: {}", path);
            File directory = new File(path.toAbsolutePath().toString());
            nodes = Arrays.stream(Objects.requireNonNull(directory.list()))
                    .map(path::resolve)
                    .peek(resolvedPath -> logger.trace("Found file in directory: {}", resolvedPath))
                    .map(builder::buildKustomFile);
        }

        return cleanse(nodes);
    }

    private Stream<GraphNode> cleanse(Stream<? extends GraphNode> nodes) {
        return nodes
                .filter(Objects::nonNull)
                .peek(node -> logger.trace("Cleansed node: {}", node.getPath()))
                .map(node -> (GraphNode) node);
    }
}