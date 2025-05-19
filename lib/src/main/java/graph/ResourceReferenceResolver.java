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

import model.Kustomization;
import model.ResourceReference;
import parser.ReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import parser.YamlParser;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class ResourceReferenceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ResourceReferenceResolver.class);
    KustomGraphBuilder builder;

    public ResourceReferenceResolver(KustomGraphBuilder builder) {
        this.builder = builder;
    }

    ResourceReference resolveDependency(ReferenceType type, Path path) {
        logger.debug("Resolving dependency of type '{}' at path: {}", type, path);

        if (YamlParser.isValidKustomizationFile(path)) {
            logger.debug("Building Kustomization: {}", path);
            return new ResourceReference(type, builder.buildKustomization(path));
        }

        logger.debug("Building KustomFile: {}", path);
        return new ResourceReference(type, builder.buildKustomFile(path));
    }

    Stream<ResourceReference> resolveDependencies(Kustomization kustomization) {
        logger.debug("Resolving dependencies for Kustomization: {}", kustomization.getPath());
        Map<String, Object> fileContent = kustomization.getContent();

        return fileContent.keySet().stream()
                .filter(ReferenceType::isReference)
                .map(ReferenceType::fromYamlKey)
                .peek(referenceType -> logger.debug("Processing reference type: {}", referenceType))
                .flatMap(referenceType ->
                        referenceType
                                .getRawReferences(fileContent)
                                .flatMap(reference -> referenceType.extract(reference, kustomization.getPath().getParent()))
                                .filter(referenceType::validate)
                                .map(reference -> resolveDependency(referenceType, reference))
                );
    }
}