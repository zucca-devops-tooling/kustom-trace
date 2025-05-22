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
package dev.zucca_ops.kustomtrace.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Represents a Kustomization (typically a kustomization.yaml file) as a node
 * in the Kustomize dependency graph. It holds its own content and references
 * to other graph nodes.
 */
public class Kustomization extends GraphNode {
    // List of resources/bases/components etc., that this kustomization references.
    private final List<ResourceReference> references = new ArrayList<>();
    // The parsed content (YAML map) of the kustomization file.
    private final Map<String, Object> content;

    private static final Logger logger = LoggerFactory.getLogger(Kustomization.class);

    /**
     * Constructs a Kustomization node.
     * @param path The file system path to this kustomization file.
     * @param content The parsed YAML content of this kustomization file.
     */
    public Kustomization(Path path, Map<String, Object> content) {
        super(path);
        this.content = Objects.requireNonNull(content, "Kustomization content cannot be null.");
    }

    /**
     * Returns the parsed content (YAML map) of this kustomization file.
     * @return The raw content map.
     */
    public Map<String, Object> getContent() {
        return Map.copyOf(content);
    }

    /**
     * Adds a resolved reference to another graph node.
     * @param reference The {@link ResourceReference} to add.
     */
    public void addReference(ResourceReference reference) {
        if (reference != null) {
            this.references.add(reference);
        }
    }

    /**
     * Gets the list of resolved references to other graph nodes.
     * @return An unmodifiable list of {@link ResourceReference}s.
     */
    public List<ResourceReference> getReferences() {
        return Collections.unmodifiableList(references); // Return a read-only view
    }

    /**
     * Returns the kind of this graph node.
     * @return Always "Kustomization" for this class.
     */
    public String getKind() { // Consider if this should be an @Override if GraphNode defines it
        return "Kustomization";
    }

    /**
     * Determines if this Kustomization is a root node (i.e., no other Kustomizations depend on it).
     * @return {@code true} if this Kustomization has no dependents, {@code false} otherwise.
     */
    @Override
    public boolean isRoot() {
        return dependents.isEmpty();
    }

    /**
     * Provides a display name for this Kustomization, typically its kind and path.
     * @return A string representation for display purposes.
     */
    public String getDisplayName() {
        return getKind() + " " + path.toString().replace(java.io.File.separator, "/");
    }

    /**
     * Recursively collects all file path dependencies for this Kustomization,
     * including its own path and the paths of all unique resources it references,
     * handling circular dependencies.
     * @param visited A set of already visited {@link GraphNode}s to prevent infinite loops
     * in circular dependencies.
     * @return A stream of unique {@link Path}s this Kustomization depends on.
     */
    @Override
    Stream<Path> getDependencies(Set<GraphNode> visited) {
        if (!visited.add(this)) {
            logger.warn("Circular dependency detected while getting dependencies for kustomization: {}", this.getPath());
            return Stream.empty();
        }

        Stream<Path> referencedDependencies = references.stream()
                .map(ResourceReference::resource)
                .filter(Objects::nonNull)
                .flatMap(resourceNode -> resourceNode.getDependencies(visited));

        // This Kustomization file itself is also part of its "dependencies" or "files involved".
        return Stream.concat(Stream.of(this.path), referencedDependencies).distinct();
    }

    /**
     * Indicates that this node is a Kustomization.
     * @return Always {@code true}.
     */
    @Override
    public boolean isKustomization() {
        return true;
    }

    /**
     * Indicates that this node is not a KustomFile.
     * @return Always {@code false}.
     */
    @Override
    public boolean isKustomFile() {
        return false;
    }

    /**
     * Retrieves all file dependencies for this Kustomization.
     * @return A stream of unique {@link Path}s this Kustomization depends on.
     */
    @Override
    public Stream<Path> getDependencies() {
        return getDependencies(new HashSet<>());
    }

    /**
     * Recursively finds all "root" Kustomizations that this Kustomization is part of
     * or contributes to. If this Kustomization itself is a root, it returns itself.
     * Otherwise, it traverses up through its dependents.
     * Handles circular dependencies.
     * @param visited A set of already visited {@link GraphNode}s to prevent infinite loops.
     * @return A stream of root {@link Kustomization}s.
     */
    private Stream<Kustomization> getApps(Set<GraphNode> visited) {
        if (!visited.add(this)) {
            logger.warn("Circular dependency detected while getting apps for kustomization: {}", this.getPath());
            return Stream.empty();
        }

        if (this.isRoot()) {
            return Stream.of(this);
        }

        return getDependents().stream()
                .flatMap(dependentKustomization -> dependentKustomization.getApps(visited));
    }

    /**
     * Retrieves all "root" Kustomizations that this Kustomization contributes to.
     * @return A stream of root {@link Kustomization}s.
     */
    @Override
    public Stream<Kustomization> getApps() {
        return getApps(new HashSet<>());
    }
}