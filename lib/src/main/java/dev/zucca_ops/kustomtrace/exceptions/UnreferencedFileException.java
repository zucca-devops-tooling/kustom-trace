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
 * Exception thrown when a file path is processed or expected to be part of the
 * Kustomize graph (e.g., a modified file input to 'affected-apps') but is found
 * to be unreferenced by any Kustomization within the analyzed structure.
 */
public class UnreferencedFileException extends KustomException {

    /**
     * Constructs an UnreferencedFileException with a default message
     * indicating the file at the specified path is not referenced.
     *
     * @param path The file system {@link Path} of the unreferenced file.
     * May be null if the context for the path is unavailable, though typically non-null.
     */
    public UnreferencedFileException(Path path) {
        super(
                "File with path "
                        + (path != null ? path : "unknown")
                        + " is not referenced by any app",
                path);
    }

    /**
     * Constructs an UnreferencedFileException with an associated path, and cause.
     *
     * @param path    The file system {@link Path} related to the exception. May be null.
     * @param cause   The underlying cause of this exception.
     */
    public UnreferencedFileException(Path path, Throwable cause) {
        super(
                "File with path "
                        + (path != null ? path : "unknown")
                        + " is not referenced by any app",
                path,
                cause);
    }
}
