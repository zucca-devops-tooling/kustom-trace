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
package model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    abstract Stream<Path> getDependencies(Set<GraphNode> visited);

    public void addDependent(Kustomization dependent) {
        dependents.add(dependent);
    }
    public abstract boolean isKustomization();
    public abstract boolean isKustomFile();
}
