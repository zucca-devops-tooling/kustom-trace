package model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class GraphNode {
    protected Path path;

    protected List<Kustomization> dependents = new ArrayList<>();
    public Path getPath() {
        return path;
    }

    protected List<Kustomization> getDependents() {
        return dependents;
    }

    public boolean hasDependent(Kustomization k) {
        return getApps().anyMatch(app -> app == k);
    }

    abstract Stream<Kustomization> getApps();
    abstract Boolean isRoot();
    abstract Stream<Path> getDependencies();

    public void addDependent(Kustomization dependent) {
        dependents.add(dependent);
    }
}
