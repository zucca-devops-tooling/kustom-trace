package model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class KustomFile extends GraphNode {

    private final List<KustomResource> resources = new ArrayList<>();

    public KustomFile(Path path) {
        this.path = path;
    }

    @Override
    Stream<Kustomization> getApps() {
        return getDependents().stream()
                .flatMap(Kustomization::getApps);
    }

    @Override
    Boolean isRoot() {
        return false;
    }

    @Override
    Stream<Path> getDependencies() {
        return Stream.of(path);
    }

    public KustomResource getResource() {
        return resources.stream().findAny().orElseGet(() -> new KustomResource("Undefined", "Undefined", this));
    }

    public List<KustomResource> getResources() {
        return resources;
    }

    public void addResource(KustomResource resource) {
        resources.add(resource);
    }
}
