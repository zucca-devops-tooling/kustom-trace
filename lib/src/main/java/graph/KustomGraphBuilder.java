package graph;

import exceptions.InvalidContentException;
import model.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;


public class KustomGraphBuilder {
    private final Path appsDir;
    private final KustomGraph graph;
    private final ResourceReferenceResolver dependencyResolver;

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

    Stream<Kustomization> buildKustomization(Path path) {
        if (!graph.containsNode(path)) {
            try {
                Kustomization k = GraphNodeResolver.resolveKustomization(path);
                graph.addNode(k);

                dependencyResolver.resolveDependencies(k)
                        .forEach(reference -> setMutualReference(k, reference));
            } catch (InvalidContentException e) {
                // TODO: log invalid kustomization.yaml content
                return Stream.empty();
            }
        }

        return Stream.of((Kustomization) graph.getNode(path));
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
