package model;

import parser.ReferenceType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class Kustomization extends GraphNode {
    private final List<ResourceReference> references = new ArrayList<>();
    private final Map<String, Object> content;

    public Kustomization(Path path, Map<String, Object> content) {
        this.path = path;
        this.content = content;
    }

    public Map<String, Object> getContent() {
        return content;
    }

    public void addReference(ResourceReference reference) {
        references.add(reference);
    }

    public List<ResourceReference> getReferences() {
        return references;
    }

    @Override
    public Stream<Kustomization> getApps() {
        if (this.isRoot()) {
            return Stream.of(this);
        }

        return getDependents().stream()
                .flatMap(Kustomization::getApps);
    }

    public String getKind() {
        return "Kustomization";
    }

    @Override
    public Boolean isRoot() {
        return dependents.isEmpty();
    }

    public String getDisplayName() {
        return getKind() + " " + path.toString();
    }

    @Override
    Stream<Path> getDependencies() {
        Stream<Path> dependencies =  references.stream()
                .map(ResourceReference::resource)
                .flatMap(GraphNode::getDependencies);

        return Stream.concat(dependencies, Stream.of(path));
    }
}
