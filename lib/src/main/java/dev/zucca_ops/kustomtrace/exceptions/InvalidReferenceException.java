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
import java.nio.file.Paths;

/**
 * Exception thrown when a reference in a Kustomization file is invalid,
 * unresolvable, or malformed.
 * <p>
 * This exception can distinguish between errors and warnings via the {@link #isError()} flag,
 * allowing for more nuanced error reporting or handling.
 */
public class InvalidReferenceException extends KustomException {

    // Indicates if this invalid reference should be treated as a hard error or a warning.
    // Defaults to false (warning) if not specified.
    private boolean isError = false;

    /**
     * Constructs an InvalidReferenceException with a message.
     * The associated path defaults to a placeholder "invalid" path.
     * Assumes this is a warning by default.
     *
     * @param message The detail message.
     */
    public InvalidReferenceException(String message) {
        super(
                message,
                Paths.get("invalid")); // Path indicates no specific file context for this error
        // isError remains false (warning)
    }

    /**
     * Constructs an InvalidReferenceException with a message and the path related to the invalid reference.
     * Assumes this is a warning by default.
     *
     * @param message The detail message.
     * @param path    The {@link Path} of the file or reference causing the issue.
     */
    public InvalidReferenceException(String message, Path path) {
        super(message, path);
        // isError remains false (warning)
    }

    /**
     * Constructs an InvalidReferenceException with a message, path, and an explicit error/warning flag.
     *
     * @param message The detail message.
     * @param path    The {@link Path} of the file or reference causing the issue.
     * @param isError {@code true} if this should be treated as a critical error,
     * {@code false} if it's a warning or less severe issue.
     */
    public InvalidReferenceException(String message, Path path, boolean isError) {
        super(message, path);
        this.isError = isError;
    }

    /**
     * Constructs an InvalidReferenceException with a message, path, and a causing throwable.
     * Assumes this is an error by default when a cause is provided.
     *
     * @param message The detail message.
     * @param path    The {@link Path} of the file or reference causing the issue.
     * @param cause   The underlying cause of this exception.
     */
    public InvalidReferenceException(String message, Path path, Throwable cause) {
        super(message, path, cause);
    }

    /**
     * Checks if this invalid reference represents a critical error.
     *
     * @return {@code true} if this instance represents an error, {@code false} if it
     * should be treated as a warning or less severe issue.
     */
    public boolean isError() {
        return isError;
    }
}
