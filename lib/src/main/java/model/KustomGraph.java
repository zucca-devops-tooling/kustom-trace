package model;

import exceptions.KustomException;
import exceptions.NotAnAppException;
import exceptions.UnreferencedFileException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KustomGraph {

    private final Map<Path, GraphNode> nodeIndex = new ConcurrentHashMap<>();

    public void addNode(GraphNode node) {
        nodeIndex.put(node.getPath().toAbsolutePath().normalize(), node);
    }

    public GraphNode getNode(Path path) {
        return nodeIndex.get(path.toAbsolutePath().normalize());
    }

    public Boolean containsNode(Path path) {
        return nodeIndex.containsKey(path.toAbsolutePath().normalize());
    }

    public List<Kustomization> getApps() {
        return nodeIndex.values().stream()
                .filter(GraphNode::isRoot)
                .map(node -> (Kustomization) node)
                .toList();
    }

    public List<Kustomization> getAppsWith(Path path) throws UnreferencedFileException {
        Path normalizedPath = path.toAbsolutePath().normalize();

        if (nodeIndex.containsKey(normalizedPath)) {
            return nodeIndex.get(normalizedPath).getApps().toList();
        }

        throw new UnreferencedFileException(normalizedPath);
    }

    public List<Path> getAppDependencies(Path path) throws KustomException {
        Path normalizedPath = path.toAbsolutePath().normalize();

        if (nodeIndex.containsKey(normalizedPath)) {
            GraphNode app = nodeIndex.get(normalizedPath);

            if (app.isRoot()) {
                return app.getDependencies().toList();
            }
            throw new NotAnAppException(normalizedPath);
        }

        throw new UnreferencedFileException(normalizedPath);
    }
}
