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
import dev.zucca_ops.kustomtrace.model.Kustomization;
import dev.zucca_ops.kustomtrace.model.ResourceReference;
import dev.zucca_ops.kustomtrace.parser.KustomizeFileUtil;
import dev.zucca_ops.kustomtrace.parser.ReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Resolves references found within a {@link Kustomization} file's content.
 * It uses a {@link KustomGraphBuilder} to build or retrieve nodes (Kustomizations or KustomFiles)
 * corresponding to these references.
 */
public class ResourceReferenceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ResourceReferenceResolver.class);
    private final KustomGraphBuilder builder; // Renamed for clarity from user's 'builder'

    /**
     * Constructs a ResourceReferenceResolver.
     *
     * @param kustomGraphBuilder The {@link KustomGraphBuilder} instance used to build
     * or retrieve graph nodes for resolved references.
     */
    public ResourceReferenceResolver(KustomGraphBuilder kustomGraphBuilder) {
        this.builder = Objects.requireNonNull(kustomGraphBuilder, "KustomGraphBuilder cannot be null.");
    }

    /**
     * Resolves a single file path (expected to be either a Kustomization file or a KustomFile)
     * into a {@link ResourceReference}.
     *
     * @param type The {@link ReferenceType} of the dependency being resolved.
     * @param path The {@link Path} to the kustomization or resource file.
     * @return A {@link ResourceReference} linking the type to the resolved graph node,
     * or {@code null} if resolution fails (e.g., due to parsing errors or file not found).
     */
    ResourceReference resolveDependency(ReferenceType type, Path path) {
        logger.debug("Resolving dependency of type '{}' at path: {}", type, path);
        try {
            // The 'path' here is expected to be a path to a specific file,
            // as resolved by the ReferenceExtractor.
            if (KustomizeFileUtil.isKustomizationFileName(path)) {
                logger.debug("Attempting to build as Kustomization: {}", path);
                return new ResourceReference(type, builder.buildKustomization(path));
            } else {
                logger.debug("Attempting to build as KustomFile: {}", path);
                return new ResourceReference(type, builder.buildKustomFile(path));
            }
        } catch (InvalidContentException e) {
            logger.error("Error parsing content for dependency type '{}' at {}: {}", type, path, e.getMessage());
            return null;
        } catch (FileNotFoundException e) {
            logger.error("File not found for dependency type '{}' at {}: {}", type, path, e.getMessage());
            return null;
        } catch (Exception e) {
            logger.error("Unexpected error resolving dependency type '{}' at {}: {}", type, path, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Resolves all known reference types (bases, resources, components, etc.)
     * found within the content of a given {@link Kustomization}.
     *
     * @param kustomization The {@link Kustomization} object whose content is to be processed.
     * @return A {@link Stream} of {@link ResourceReference} objects representing all
     * successfully resolved dependencies. Returns an empty stream if the
     * kustomization is null, has null content, or no valid references are found.
     */
    public Stream<ResourceReference> resolveDependencies(Kustomization kustomization) {
        if (kustomization == null || kustomization.getContent() == null) {
            logger.warn("Attempted to resolve dependencies for a null Kustomization or Kustomization with null content.");
            return Stream.empty();
        }

        logger.debug("Resolving dependencies for Kustomization: {}", kustomization.getPath());
        Map<String, Object> fileContent = kustomization.getContent();
        Path baseDir = kustomization.getPath().getParent();
        if (baseDir == null) {
            // This might happen if the kustomization path is a root path itself (e.g. "kustomization.yaml" in CWD)
            // In such cases, baseDir for resolving relative paths would be the current directory.
            baseDir = Path.of("").toAbsolutePath();
            logger.warn("Kustomization path {} has no parent, using current working directory '{}' as baseDir for resolving references.", kustomization.getPath(), baseDir);
        }

        final Path finalBaseDir = baseDir;

        return fileContent.keySet().stream()
                .filter(ReferenceType::isReferenceKey) // Find keys that match known reference types
                .map(ReferenceType::fromYamlKey)       // Convert key string to ReferenceType enum
                .peek(referenceType -> logger.debug("Processing Kustomization key: '{}' as ReferenceType: {}", referenceType.getYamlKey(), referenceType))
                .flatMap(referenceType -> {
                            // Get the raw values (e.g., list of strings) for this key
                            return referenceType.getRawReferences(fileContent)
                                    .flatMap(rawReferenceValue -> referenceType.extract(rawReferenceValue, finalBaseDir))
                                    .map(resolvedPath -> resolveDependency(referenceType, resolvedPath))
                                    .filter(Objects::nonNull); // Filter out any nulls from failed resolveDependency calls
                        }
                );
    }
}