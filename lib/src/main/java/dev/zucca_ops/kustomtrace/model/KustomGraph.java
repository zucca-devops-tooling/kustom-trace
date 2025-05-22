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

import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import dev.zucca_ops.kustomtrace.exceptions.UnreferencedFileException;
import dev.zucca_ops.kustomtrace.parser.KustomizeFileUtil;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class KustomGraph {

    private final Map<Path, GraphNode> nodeIndex = new ConcurrentHashMap<>();

    public void addNode(GraphNode node) {
        nodeIndex.put(node.getPath().toAbsolutePath().normalize(), node);
    }

    public GraphNode getNode(Path path) {
        return nodeIndex.get(path.toAbsolutePath().normalize());
    }

    public Boolean containsNode(Path path) {
        return nodeIndex.containsKey(path.toAbsolutePath().normalize());
    }

    public List<Kustomization> getRootApps() {
        return nodeIndex.values().stream()
                .filter(GraphNode::isRoot)
                .map(node -> (Kustomization) node)
                .toList();
    }

    public List<Kustomization> getRootAppsWithFile(Path path) throws UnreferencedFileException {
        Path normalizedPath = path.toAbsolutePath().normalize();

        if (nodeIndex.containsKey(normalizedPath)) {
            return nodeIndex.get(normalizedPath).getApps().toList();
        }

        throw new UnreferencedFileException(normalizedPath);
    }

    public List<Path> getAllAppFiles(Path path) throws KustomException {
        if (path == null) {
            throw new IllegalArgumentException("Input path for getAllAppFiles cannot be null.");
        }
        Path normalizedPath = KustomizeFileUtil.getKustomizationFileFromAppDirectory(path).toAbsolutePath().normalize();

        if (nodeIndex.containsKey(normalizedPath)) {
            GraphNode app = nodeIndex.get(normalizedPath);

            return app.getDependencies().toList();
        }

        throw new UnreferencedFileException(normalizedPath);
    }

    public KustomFile getKustomFile(Path path) {
        GraphNode node = getNode(path);

        if (node.isKustomFile()) {
            return (KustomFile) node;
        }

        return null;
    }

    public Kustomization getKustomization(Path path) {
        GraphNode node = getNode(path);

        if (node.isKustomization()) {
            return (Kustomization) node;
        }

        return null;
    }
}
