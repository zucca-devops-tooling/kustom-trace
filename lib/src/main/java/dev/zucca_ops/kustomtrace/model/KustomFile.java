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
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents a regular Kubernetes manifest file (e.g., YAML or JSON)
 * as a node in the Kustomize dependency graph. It can contain one or more
 * Kubernetes resources.
 */
public class KustomFile extends GraphNode {

    // List of Kubernetes resources parsed from this file.
    private final List<KustomResource> resources = new ArrayList<>();

    /**
     * Constructs a KustomFile node.
     * @param path The file system path to this file.
     */
    public KustomFile(Path path) {
        super(path);
    }

    /**
     * Retrieves the root Kustomizations that ultimately reference this file.
     * This is determined by traversing up through its dependents.
     * @return A stream of root {@link Kustomization}s that depend on this file.
     */
    @Override
    Stream<Kustomization> getApps() {
        return getDependents().stream()
                .flatMap(Kustomization::getApps);
    }

    /**
     * @return KustomFile is never a root {@code false}.
     */
    @Override
    boolean isRoot() { // The return type in GraphNode is likely Boolean (Object)
        return false;
    }

    /**
     * @return A stream containing only the path of this KustomFile.
     */
    @Override
    Stream<Path> getDependencies() {
        return Stream.of(path);
    }

    /**
     * @return A stream containing only the path of this KustomFile.
     */
    @Override
    Stream<Path> getDependencies(Set<GraphNode> visited) {
        return getDependencies();
    }

    /**
     * Indicates that this node is not a Kustomization.
     * @return Always {@code false}.
     */
    @Override
    public boolean isKustomization() {
        return false;
    }

    /**
     * Indicates that this node is a KustomFile.
     * @return Always {@code true}.
     */
    @Override
    public boolean isKustomFile() {
        return true;
    }

    /**
     * Gets the first Kubernetes resource found in this file, or a default "Undefined" resource
     * if the file contains no parsed resources.
     * <p>
     * Note: Kustomize files can contain multiple resources. This method provides a simplified
     * way to access one, primarily for scenarios where only one is expected or any will do.
     * For all resources, use {@link #getResources()}.
     *
     * @return The first {@link KustomResource}, or an "Undefined" resource if none exist.
     */
    public KustomResource getResource() {
        return resources.stream().findAny().orElseGet(() -> new KustomResource("Undefined", "Undefined", this));
    }

    /**
     * Gets all Kubernetes resources parsed from this file.
     * @return An unmodifiable list of {@link KustomResource}s.
     */
    public List<KustomResource> getResources() {
        return resources;
    }

    /**
     * Adds a parsed Kubernetes resource to this file.
     */
    public void addResource(KustomResource resource) {
        resources.add(resource);
    }
}
