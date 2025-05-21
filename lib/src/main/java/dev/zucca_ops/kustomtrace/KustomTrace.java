package dev.zucca_ops.kustomtrace;/*
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
import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import dev.zucca_ops.kustomtrace.exceptions.UnreferencedFileException;
import dev.zucca_ops.kustomtrace.graph.KustomGraphBuilder;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class KustomTrace { // Class name 'dev.zucca_ops.kustomtrace.KustomTrace' doesn't match package 'parser'

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

    public List<Kustomization> getApps() {
        logger.debug("Getting all applications from the graph.");
        return graph.getApps();
    }

    public List<Kustomization> getAppsWith(Path file) throws UnreferencedFileException {
        logger.debug("Getting applications referencing file: {}", file);
        return graph.getAppsWith(file);
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