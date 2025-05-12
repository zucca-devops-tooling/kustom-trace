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
import exceptions.KustomException;
import exceptions.UnreferencedFileException;
import graph.KustomGraphBuilder;
import model.KustomGraph;
import model.Kustomization;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class KustomTrace {

    private final KustomGraph graph;

    private KustomTrace(KustomGraph graph) {
        this.graph = graph;
    }

    public static KustomTrace fromDirectory(Path appsDir) throws IOException {
        KustomGraphBuilder builder = new KustomGraphBuilder(appsDir);
        return new KustomTrace(builder.build());
    }

    public List<Kustomization> getApps() {
        return graph.getApps();
    }

    public List<Kustomization> getAppsWith(Path file) throws UnreferencedFileException {
        return graph.getAppsWith(file);
    }

    public List<Path> getDependenciesFor(Path app) throws KustomException {
        return graph.getAllAppFiles(app);
    }

    public KustomGraph getGraph() {
        return graph;
    }
}
