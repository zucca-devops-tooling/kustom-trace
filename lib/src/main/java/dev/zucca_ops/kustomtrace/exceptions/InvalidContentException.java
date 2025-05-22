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
 * Exception thrown when the content of a Kustomize file (e.g., kustomization.yaml,
 * or a resource YAML/JSON) is found to be invalid or unparsable according
 * to expected structure or syntax.
 */
public class InvalidContentException extends KustomException {

    /**
     * Constructs a new InvalidContentException with a default message
     * indicating invalid content for the specified file path.
     *
     * @param path The file system {@link Path} of the file with invalid content.
     */
    public InvalidContentException(Path path) {
        super("File " + (path != null ? path : "unknown") +  " has invalid Kustomize content", path);
    }

    /**
     * Constructs a new InvalidContentException with an associated path, and cause.
     *
     * @param path    The file system {@link Path} related to the exception.
     * @param cause   The underlying cause of this exception.
     */
    public InvalidContentException(Path path, Throwable cause) {
        super("File " + (path != null ? path : "unknown") +  " has invalid Kustomize content", path, cause);
    }
}
