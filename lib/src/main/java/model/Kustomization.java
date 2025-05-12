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
