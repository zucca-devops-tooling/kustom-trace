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
 * Exception thrown when a given path is expected to represent or lead to a
 * Kustomize application (i.e., a kustomization file or a directory containing one),
 * but cannot be resolved as such.
 */
public class NotAnAppException extends KustomException {

    /**
     * Constructs a NotAnAppException with a default message indicating the path
     * is not a valid Kustomize application.
     *
     * @param path The file system {@link Path} that was determined not to be a Kustomize application.
     * May be null if the context for the path is unavailable.
     */
    public NotAnAppException(Path path) {
        super("Path " + (path != null ? path : "unknown") + " does not resolve to a valid Kustomization.", path);
    }


    /**
     * Constructs a NotAnAppException with a cause.
     *
     * @param path    The file system {@link Path} related to the exception. May be null.
     * @param cause   The underlying cause of this exception.
     */
    public NotAnAppException(Path path, Throwable cause) {
        super("Path " + (path != null ? path : "unknown") + " does not resolve to a valid Kustomization.", path, cause);
    }
}
