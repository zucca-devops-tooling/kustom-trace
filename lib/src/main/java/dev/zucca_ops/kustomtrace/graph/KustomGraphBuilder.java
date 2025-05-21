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
import dev.zucca_ops.kustomtrace.parser.YamlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


public class KustomGraphBuilder {
    private final Path appsDir;
    private final KustomGraph graph;
    private final ResourceReferenceResolver dependencyResolver;
    private static final Logger logger = LoggerFactory.getLogger(KustomGraphBuilder.class);

    public KustomGraphBuilder(Path appsDir) {
        this.appsDir = appsDir;
        this.graph = new KustomGraph();
        this.dependencyResolver = new ResourceReferenceResolver(this);
        logger.info("KustomGraphBuilder initialized with apps directory: {}", appsDir);
    }

    public KustomGraph build() throws IOException {
        logger.info("Starting to build Kustom Graph from: {}", appsDir);
        AtomicInteger kustomizationCount = new AtomicInteger(0);
        try (Stream<Path> stream = Files.walk(appsDir)) {
            stream.filter(YamlParser::isValidKustomizationFile)
                    .parallel()
                    .forEach(path -> {
                        try {
                            buildKustomization(path);
                        } catch (InvalidContentException | FileNotFoundException e) {
                            logger.error("Invalid kustomization found at " + path);
                            return;
                        }
                        kustomizationCount.incrementAndGet();
                    });
        }
        logger.info("Finished walking the apps directory. Found {} kustomization.yaml files.", kustomizationCount.get());
        return graph;
    }

    Kustomization buildKustomization(Path path) throws InvalidContentException, FileNotFoundException {
        logger.debug("Building Kustomization for: {}", path);
        if (!graph.containsNode(path)) {
            logger.debug("Resolving Kustomization using GraphNodeResolver for: {}", path);
            Kustomization k = GraphNodeResolver.resolveKustomization(path);
            logger.debug("Adding Kustomization node to graph: {}", k.getPath());
            graph.addNode(k);
            logger.debug("Resolving dependencies for Kustomization: {}", k.getPath());

            dependencyResolver.resolveDependencies(k)
                    .forEach(reference -> setMutualReference(k, reference));
        } else {
            logger.debug("Kustomization node already exists for: {}", path);
        }

        return graph.getKustomization(path);
    }

    KustomFile buildKustomFile(Path path) throws InvalidContentException, FileNotFoundException {
        logger.debug("Building KustomFile for: {}", path);
        if (!graph.containsNode(path)) {
            logger.debug("Resolving KustomFile using GraphNodeResolver for: {}", path);
            KustomFile file = GraphNodeResolver.resolveKustomFile(path);
            logger.debug("Adding KustomFile node to graph: {}", file.getPath());
            graph.addNode(file);
        } else {
            logger.debug("KustomFile node already exists for: {}", path);
        }

        return graph.getKustomFile(path);
    }

    private void setMutualReference(Kustomization dependent, ResourceReference reference) {
        logger.trace("Setting mutual reference between {} and {}", dependent.getPath(), reference.resource().getPath());
        dependent.addReference(reference);
        reference.resource().addDependent(dependent);
    }
}
