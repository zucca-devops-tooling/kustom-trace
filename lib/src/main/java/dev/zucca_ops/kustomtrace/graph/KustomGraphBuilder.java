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
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import dev.zucca_ops.kustomtrace.model.ResourceReference;
import dev.zucca_ops.kustomtrace.parser.KustomizeFileUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds a {@link KustomGraph} by scanning a directory structure for Kustomization files,
 * resolving their contents, and establishing dependencies between them and other resources.
 */
public class KustomGraphBuilder {
    private final Path appsDir;
    private final KustomGraph graph;
    private final ResourceReferenceResolver dependencyResolver;
    private static final Logger logger = LoggerFactory.getLogger(KustomGraphBuilder.class);

    /**
     * Constructs a KustomGraphBuilder.
     *
     * @param appsDir The root directory to scan for Kustomize applications.
     */
    public KustomGraphBuilder(Path appsDir) {
        this.appsDir = appsDir;
        this.graph = new KustomGraph();
        // Pass 'this' builder instance to the resolver, allowing the resolver to call
        // buildKustomization/buildKustomFile for discovered references.
        this.dependencyResolver = new ResourceReferenceResolver(this);
        logger.info("KustomGraphBuilder initialized with apps directory: {}", this.appsDir);
    }

    /**
     * Scans the configured {@code appsDir}, builds all found Kustomizations and their
     * referenced resources, and populates the internal {@link KustomGraph}.
     * <p>
     * Uses a parallel stream to process kustomization files found by {@link Files#walk(Path, java.nio.file.FileVisitOption...)}.
     *
     * @return The populated {@link KustomGraph}.
     * @throws IOException If an I/O error occurs when walking the appsDir.
     */
    public KustomGraph build() throws IOException {
        logger.info("Starting to build Kustom Graph from: {}", appsDir);
        AtomicInteger kustomizationCount = new AtomicInteger(0);

        if (!Files.exists(appsDir) || !Files.isDirectory(appsDir)) {
            logger.error("Apps directory does not exist or is not a directory: {}", appsDir);
            throw new FileNotFoundException(
                    "Apps directory not found or is not a directory: " + appsDir);
        }

        try (Stream<Path> stream = Files.walk(appsDir)) {
            stream.filter(KustomizeFileUtil::isKustomizationFileName)
                    .filter(
                            KustomizeFileUtil
                                    ::isFile) // Ensure they are actual files, not directories named
                    // like kustomization.yaml
                    .parallel() // Process kustomization files in parallel
                    .forEach(
                            path -> {
                                try {
                                    buildKustomization(path);
                                    kustomizationCount.incrementAndGet();
                                } catch (InvalidContentException | FileNotFoundException e) {
                                    logger.error(
                                            "Skipping invalid or unreadable kustomization file at {}: {}",
                                            path,
                                            e.getMessage());
                                } catch (Exception e) {
                                    logger.error(
                                            "Unexpected error building kustomization for path {}: {}",
                                            path,
                                            e.getMessage(),
                                            e);
                                }
                            });
        }
        logger.info(
                "Finished directory walk. Attempted to build {} kustomization file(s).",
                kustomizationCount.get());

        return graph;
    }

    /**
     * Resolves and builds a {@link Kustomization} node for the given path.
     * If the node already exists in the graph, it's returned. Otherwise, it's resolved,
     * added to the graph, and its dependencies are processed.
     *
     * @param path The {@link Path} to the kustomization file.
     * @return The resolved or existing {@link Kustomization} node from the graph.
     * @throws InvalidContentException If the kustomization file content is invalid.
     * @throws FileNotFoundException If the kustomization file is not found.
     */
    Kustomization buildKustomization(Path path)
            throws InvalidContentException, FileNotFoundException {
        logger.debug("Building Kustomization for: {}", path);

        if (graph.containsNode(path)) {
            logger.debug("Kustomization node already exists in graph for: {}", path);
            return graph.getKustomization(path);
        }

        logger.debug("Resolving Kustomization using GraphNodeResolver for: {}", path);
        Kustomization k = GraphNodeResolver.resolveKustomization(path);

        logger.debug("Adding Kustomization node to graph: {}", k.getPath());
        graph.addNode(k); // Add to graph *before* resolving dependencies to handle cycles

        logger.debug("Resolving dependencies for Kustomization: {}", k.getPath());
        dependencyResolver
                .resolveDependencies(k)
                .forEach(reference -> setMutualReference(k, reference));

        return k;
    }

    /**
     * Resolves and builds a {@link KustomFile} node for the given path.
     * If the node already exists in the graph, it's returned. Otherwise, it's resolved
     * and added to the graph.
     *
     * @param path The {@link Path} to the resource file.
     * @return The resolved or existing {@link KustomFile} node from the graph.
     * @throws InvalidContentException If the file content is invalid (for parseable types).
     * @throws FileNotFoundException If the file is not found.
     */
    KustomFile buildKustomFile(Path path) throws InvalidContentException, FileNotFoundException {
        logger.debug("Building KustomFile for: {}", path);

        if (graph.containsNode(path)) {
            logger.debug("KustomFile node already exists in graph for: {}", path);
            return graph.getKustomFile(path);
        }

        logger.debug("Resolving KustomFile using GraphNodeResolver for: {}", path);
        KustomFile file = GraphNodeResolver.resolveKustomFile(path);

        logger.debug("Adding KustomFile node to graph: {}", file.getPath());
        graph.addNode(file);

        return file;
    }

    /**
     * Establishes a bi-directional reference between a Kustomization (dependent)
     * and the KustomNode it refers to (dependency).
     *
     * @param dependent The Kustomization that contains the reference.
     * @param reference The {@link ResourceReference} (containing the type and the target resource node).
     */
    private void setMutualReference(Kustomization dependent, ResourceReference reference) {
        logger.trace(
                "Setting mutual reference between dependent {} and resource {}",
                dependent.getPath(),
                reference.resource().getPath());
        dependent.addReference(reference);
        reference.resource().addDependent(dependent);
    }
}
