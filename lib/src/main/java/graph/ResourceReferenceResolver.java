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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class ResourceReferenceResolver {

    KustomGraphBuilder builder;

    public ResourceReferenceResolver(KustomGraphBuilder builder) {
        this.builder = builder;
    }

    Stream<ResourceReference> resolveDependency(ReferenceType type, Path path) {
        try {
            type.validate(path);
        } catch (InvalidReferenceException e) {
            //TODO: log invalid reference
            System.out.println("Invalid reference");
            System.out.println(e.getMessage());
            System.out.println(path.toString());
            return Stream.empty();
        }

        if (Files.isDirectory(path)) {
            return resolveDirectory(path)
                    .map(node -> new ResourceReference(type, node));
        }

        return Stream.of(new ResourceReference(type, builder.buildKustomFile(path)));
    }

    Stream<ResourceReference> resolveDependencies(Kustomization kustomization) {
        Map<String, Object> fileContent = kustomization.getContent();

        return fileContent.keySet().stream()
            .filter(ReferenceType::isReference)
            .map(ReferenceType::fromYamlKey)
            .flatMap(referenceType ->
                    referenceType.extract(fileContent, kustomization.getPath().getParent())
                            .flatMap(dependency -> resolveDependency(referenceType, dependency))
            );
    }

    Stream<GraphNode> resolveDirectory(Path path) {
        Path kustomization = path.resolve("kustomization.yaml");
        Stream<? extends GraphNode> nodes;

        if (Files.exists(kustomization)) {
            nodes = Stream.of(builder.buildKustomization(kustomization));
        } else {
            File directory = new File(path.toAbsolutePath().toString());
            nodes = Arrays.stream(Objects.requireNonNull(directory.list()))
                    .map(path::resolve)
                    .map(builder::buildKustomFile);
        }

        return cleanse(nodes);
    }

    private Stream<GraphNode> cleanse(Stream<? extends GraphNode> nodes) {
        return nodes
                .filter(Objects::nonNull)
                .map(node ->  (GraphNode) node);
    }
}
