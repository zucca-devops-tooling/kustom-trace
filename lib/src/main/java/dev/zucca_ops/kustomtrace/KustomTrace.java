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
package dev.zucca_ops.kustomtrace;

import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import dev.zucca_ops.kustomtrace.exceptions.UnreferencedFileException;
import dev.zucca_ops.kustomtrace.graph.KustomGraphBuilder;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KustomTrace {

    private static final Logger logger = LoggerFactory.getLogger(KustomTrace.class);
    private final KustomGraph graph;

    private KustomTrace(KustomGraph graph) {
        this.graph = graph;
        logger.debug("dev.zucca_ops.kustomtrace.KustomTrace instance created.");
    }

    public static KustomTrace fromDirectory(Path appsDir) throws IOException {
        logger.info("Creating dev.zucca_ops.kustomtrace.KustomTrace from directory: {}", appsDir);
        KustomGraphBuilder builder = new KustomGraphBuilder(appsDir);
        logger.debug("KustomGraphBuilder created for: {}", appsDir);
        KustomGraph graph = builder.build();
        logger.info("Kustom graph built successfully.");
        return new KustomTrace(graph);
    }

    public List<Path> getRootApps() {
        logger.debug("Getting all applications from the graph.");
        return kustomizationsToPath(graph.getRootApps());
    }

    private List<Path> kustomizationsToPath(List<Kustomization> apps) {
        return apps.stream().map(Kustomization::getPath).map(Path::getParent).toList();
    }

    public List<Path> getAppsWith(Path file) throws UnreferencedFileException {
        logger.debug("Getting applications referencing file: {}", file);
        return kustomizationsToPath(graph.getRootAppsWithFile(file));
    }

    public List<Path> getDependenciesFor(Path app) throws KustomException {
        logger.debug("Getting dependencies for application at: {}", app);
        return graph.getAllAppFiles(app);
    }

    public KustomGraph getGraph() {
        logger.debug("Getting the underlying KustomGraph.");
        return graph;
    }
}
