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

import java.util.Optional;

/**
 * Represents a single Kubernetes resource (e.g., Deployment, Service)
 * defined within a {@link KustomFile}.
 */
public class KustomResource {

    private String name;
    private String kind;
    private KustomFile file; // Back-reference to the file containing this resource

    /**
     * Default constructor.
     * Name, kind, and file should be set via setters or a parameterized constructor.
     */
    public KustomResource() {
    }

    /**
     * Constructs a KustomResource with specified name, kind, and containing file.
     * @param name The name of the Kubernetes resource (from metadata.name).
     * @param kind The kind of the Kubernetes resource (e.g., Deployment, Service).
     * @param file The {@link KustomFile} this resource was parsed from.
     */
    public KustomResource(String name, String kind, KustomFile file) {
        this.name = name;
        this.kind = kind;
        this.file = file;
    }

    /**
     * @return The kind of the Kubernetes resource (e.g., "Deployment", "Service").
     * May be null if not set or not found during parsing.
     */
    public String getKind() {
        return kind;
    }

    /**
     * Sets the kind of this Kubernetes resource.
     * @param kind The resource kind.
     */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /**
     * Sets the name of this Kubernetes resource.
     * @param name The resource name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the {@link KustomFile} that contains this resource.
     * @param file The parent KustomFile.
     */
    public void setFile(KustomFile file) {
        this.file = file;
    }

    /**
     * Generates a display name for the resource, combining its kind, name, and file path.
     * Uses forward slashes for path separators.
     * @return A string suitable for display.
     */
    public String getDisplayName() {
        String kindDisplay = Optional.ofNullable(this.kind).orElse("UndefinedKind");
        String nameDisplay = getName();

        String filePathDisplay = "unknown-file";
        if (file != null && file.getPath() != null) {
            filePathDisplay = file.getPath().toString().replace(java.io.File.separator, "/");
        }

        return String.format("%s %s (in %s)", kindDisplay, nameDisplay, filePathDisplay);
    }

    /**
     * @return The name of the Kubernetes resource (from metadata.name).
     * Returns "undefined" if the name is null.
     */
    public String getName() {
        return Optional.ofNullable(name).orElse("undefined");
    }

    /**
     * @return The {@link KustomFile} that contains this resource. May be null if not set.
     */
    public KustomFile getFile() {
        return file;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}