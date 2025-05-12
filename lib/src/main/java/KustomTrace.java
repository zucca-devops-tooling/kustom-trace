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
