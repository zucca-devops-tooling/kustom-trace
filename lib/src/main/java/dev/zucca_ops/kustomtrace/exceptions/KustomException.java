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
package dev.zucca_ops.kustomtrace.exceptions;

import java.nio.file.Path;

/**
 * Base class for custom exceptions specific to the KustomTrace application.
 * Typically indicates an issue encountered during Kustomize structure parsing or graph building.
 */
public abstract class KustomException extends Exception { // Mark as abstract if it's only meant to be subclassed

    /**
     * The file system path primarily associated with this exception, if applicable.
     * Can be null if the exception is not specific to a single file path.
     */
    protected Path path; // Consider making this final if set only at construction

    /**
     * Constructs a new KustomException with the specified detail message and associated path.
     *
     * @param message The detail message.
     * @param path    The file system {@link Path} related to the exception, may be null.
     */
    public KustomException(String message, Path path) {
        super(message);
        this.path = path;
    }

    /**
     * Constructs a new KustomException with the path, and cause.
     *
     * @param message The detail message.
     * @param path    The file system {@link Path} related to the exception, may be null.
     * @param cause   The cause of this exception.
     */
    public KustomException(String message, Path path, Throwable cause) {
        super(message, cause);
        this.path = path;
    }

    /**
     * Gets the file system path associated with this exception.
     *
     * @return The {@link Path}, or {@code null} if no specific path is associated.
     */
    public Path getPath() {
        return path;
    }
}