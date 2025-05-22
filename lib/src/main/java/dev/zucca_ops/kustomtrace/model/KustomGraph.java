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
import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import dev.zucca_ops.kustomtrace.exceptions.UnreferencedFileException;
import dev.zucca_ops.kustomtrace.parser.KustomizeFileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the complete graph of Kustomize resources and their relationships.
 * It maintains an index of all discovered {@link GraphNode}s (Kustomizations and KustomFiles).
 */
public class KustomGraph {
    private static final Logger logger = LoggerFactory.getLogger(KustomGraph.class);

    // Using ConcurrentHashMap for thread-safe access if nodes are added from parallel streams.
    private final Map<Path, GraphNode> nodeIndex = new ConcurrentHashMap<>();

    /**
     * Adds a {@link GraphNode} (like a {@link Kustomization} or {@link KustomFile}) to the graph.
     * The node is indexed by its absolute, normalized path.
     * If a node with the same path already exists, it will be replaced.
     *
     * @param node The {@link GraphNode} to add. Must not be null and must have a valid path.
     */
    public void addNode(GraphNode node) {
        if (node == null || node.getPath() == null) {
            logger.error("Attempted to add a null node or node with a null path to KustomGraph.");
            return;
        }
        nodeIndex.put(node.getPath().toAbsolutePath().normalize(), node);
    }

    /**
     * Retrieves a {@link GraphNode} from the graph by its path.
     *
     * @param path The path of the node to retrieve.
     * @return The {@link GraphNode} if found, or {@code null} if no node exists for the given path.
     */
    public GraphNode getNode(Path path) {
        if (path == null) return null;
        return nodeIndex.get(path.toAbsolutePath().normalize());
    }

    /**
     * Checks if a node with the given path exists in the graph.
     *
     * @param path The path to check.
     * @return {@code true} if a node with the path exists, {@code false} otherwise.
     */
    public boolean containsNode(Path path) {
        if (path == null) return false;
        return nodeIndex.containsKey(path.toAbsolutePath().normalize());
    }

    /**
     * Gets all Kustomizations in the graph that are considered "root" applications
     * (i.e., not depended upon by any other Kustomization).
     *
     * @return A list of root {@link Kustomization}s.
     */
    public List<Kustomization> getRootApps() {
        return nodeIndex.values().stream()
                .filter(GraphNode::isRoot)
                .map(Kustomization.class::cast)
                .toList();
    }

    /**
     * Gets all root Kustomizations that directly or indirectly reference the given file path.
     *
     * @param path The file path to check for references.
     * @return A list of root {@link Kustomization}s that depend on the given file.
     * @throws UnreferencedFileException if the given path is not found as a node in the graph.
     */
    public List<Kustomization> getRootAppsWithFile(Path path) throws UnreferencedFileException {
        if (path == null) {
            throw new IllegalArgumentException("Path cannot be null for getRootAppsWithFile.");
        }
        Path normalizedPath = path.toAbsolutePath().normalize();

        GraphNode node = nodeIndex.get(normalizedPath);
        if (node != null) {
            return node.getApps().toList();
        }
        throw new UnreferencedFileException(normalizedPath);
    }

    /**
     * Retrieves all file dependencies for a given application path.
     * The input path can be a directory containing a kustomization file or a direct
     * path to a kustomization file.
     *
     * @param appPath The path representing the application.
     * @return A list of all unique {@link Path}s that the application depends on, including itself.
     * @throws KustomException If the appPath is not a valid Kustomize application or other processing errors occur.
     * @throws IllegalArgumentException if appPath is null.
     */
    public List<Path> getAllAppFiles(Path appPath) throws KustomException {
        if (appPath == null) {
            throw new IllegalArgumentException("Input application path for getAllAppFiles cannot be null.");
        }

        Path actualKustomizationFilePath = KustomizeFileUtil.getKustomizationFileFromAppDirectory(appPath);
        Path normalizedKeyPath = actualKustomizationFilePath.toAbsolutePath().normalize();

        GraphNode appNode = nodeIndex.get(normalizedKeyPath);
        if (appNode instanceof Kustomization) { // Ensure it's a Kustomization before calling getDependencies
            return appNode.getDependencies().toList();
        } else if (appNode != null) { // Node exists but is not a Kustomization
            throw new NotAnAppException(appPath);
        }

        // If appNode is null, it means KustomizeFileUtil found a kustomization file path,
        // but it wasn't added to the graph (e.g., during a parallel build where it wasn't processed yet, or error).
        // Or KustomGraphBuilder didn't process this specific root.
        throw new UnreferencedFileException(normalizedKeyPath);
    }


    /**
     * Retrieves a {@link KustomFile} from the graph, ensuring type safety.
     *
     * @param path The path of the KustomFile to retrieve.
     * @return The {@link KustomFile} if found and is of the correct type, or {@code null} otherwise.
     */
    public KustomFile getKustomFile(Path path) {
        GraphNode node = getNode(path);
        if (node != null && node.isKustomFile()) {
            return (KustomFile) node;
        }

        logger.warn("Attempted to get a non KustomFile at " + path);
        return null;
    }

    /**
     * Retrieves a {@link Kustomization} from the graph, ensuring type safety.
     *
     * @param path The path of the Kustomization to retrieve.
     * @return The {@link Kustomization} if found and is of the correct type, or {@code null} otherwise.
     */
    public Kustomization getKustomization(Path path) {
        GraphNode node = getNode(path);
        if (node != null && node.isKustomization()) {
            return (Kustomization) node;
        }

        logger.warn("Attempted to get a non Kustomization at " + path);
        return null;
    }
}
