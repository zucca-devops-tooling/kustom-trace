package model;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public abstract class GraphNode {
    protected Path path;

    protected List<Kustomization> dependents;
    public Path getPath() {
        return path;
    }

    protected List<Kustomization> getDependents() {
        return dependents;
    }

    abstract Stream<Kustomization> getApps();
    abstract Boolean isRoot();
    abstract Stream<Path> getDependencies();

    public void addDependent(Kustomization dependent) {
        dependents.add(dependent);
    }
}
