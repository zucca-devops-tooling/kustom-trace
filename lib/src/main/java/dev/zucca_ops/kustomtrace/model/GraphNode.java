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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Abstract base class for nodes in the Kustomize dependency graph.
 * Represents either a Kustomization file or a Kubernetes resource file.
 */
public abstract class GraphNode {
    protected Path path; // Path to the file system entity this node represents.

    // Kustomizations that depend on this node.
    protected List<Kustomization> dependents = new ArrayList<>();

    /**
     * Constructor to set the path for the graph node.
     * @param path The file system path this node represents.
     */
    protected GraphNode(Path path) {
        this.path = path;
    }

    /**
     * @return The {@link Path} to the file system entity this node represents.
     */
    public Path getPath() {
        return path;
    }

    /**
     * @return An unmodifiable list of Kustomizations that depend on this node.
     */
    protected List<Kustomization> getDependents() {
        return Collections.unmodifiableList(dependents); // Return read-only view
    }

    /**
     * Checks if the given Kustomization is a direct or indirect root application
     * that this node contributes to.
     * @param k The Kustomization to check.
     * @return {@code true} if k is an application this node is part of, {@code false} otherwise.
     */
    public boolean hasDependent(Kustomization k) {
        return getApps().anyMatch(app -> app == k);
    }

    /**
     * Adds a Kustomization that depends on this node.
     * @param dependent The Kustomization that depends on this node.
     */
    public void addDependent(Kustomization dependent) {
        if (dependent != null && !this.dependents.contains(dependent)) { // Avoid duplicates
            this.dependents.add(dependent);
        }
    }

    // --- Abstract methods to be implemented by subclasses ---

    /**
     * Finds all root Kustomizations that this node is a part of.
     * @return A stream of root {@link Kustomization}s.
     */
    abstract Stream<Kustomization> getApps();

    /**
     * Indicates if this node is a root of a Kustomize application tree.
     * @return {@code true} if it's a root, {@code false} otherwise.
     */
    abstract boolean isRoot();

    /**
     * Gets all file paths this node directly or indirectly depends on (including itself).
     * @return A stream of {@link Path}s.
     */
    abstract Stream<Path> getDependencies();

    /**
     * Gets all file paths this node directly or indirectly depends on (including itself),
     * using a provided set to detect and prevent cycles.
     * @param visited A set of already visited nodes in the current traversal.
     * @return A stream of {@link Path}s.
     */
    abstract Stream<Path> getDependencies(Set<GraphNode> visited);

    /**
     * @return {@code true} if this node represents a Kustomization file.
     */
    public abstract boolean isKustomization();

    /**
     * @return {@code true} if this node represents a regular KustomFile (not a Kustomization).
     */
    public abstract boolean isKustomFile();
}