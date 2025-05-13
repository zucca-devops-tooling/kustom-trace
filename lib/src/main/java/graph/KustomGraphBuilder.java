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
import model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    }

    public KustomGraph build() throws IOException {
        Files.walk(appsDir)
                .filter(path -> path.getFileName().toString().equals("kustomization.yaml"))
                .parallel()
                .forEach(this::buildKustomization);

        return graph;
    }

    Kustomization buildKustomization(Path path) {
        if (!graph.containsNode(path)) {
            try {
                Kustomization k = GraphNodeResolver.resolveKustomization(path);
                graph.addNode(k);

                dependencyResolver.resolveDependencies(k)
                        .forEach(reference -> setMutualReference(k, reference));
            } catch (InvalidContentException e) {
                // TODO: log invalid kustomization.yaml content
                logger.warn(e.getMessage());
                return null;
            }
        }

        return (Kustomization) graph.getNode(path);
    }

    KustomFile buildKustomFile(Path path) {
        if (!graph.containsNode(path)) {
            graph.addNode(
                    GraphNodeResolver.resolveKustomFile(path)
            );
        }

        return (KustomFile) graph.getNode(path);
    }

    private void setMutualReference(Kustomization dependent, ResourceReference reference) {
        dependent.addReference(reference);
        reference.resource().addDependent(dependent);
    }
}
