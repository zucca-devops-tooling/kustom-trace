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
    public Stream<Path> getDependencies() {
        return Stream.of(path);
    }

    @Override
    Stream<Path> getDependencies(Set<GraphNode> visited) {
        return getDependencies();
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
