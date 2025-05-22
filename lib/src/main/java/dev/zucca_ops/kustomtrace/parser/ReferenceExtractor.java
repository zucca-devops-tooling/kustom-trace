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
package dev.zucca_ops.kustomtrace.parser;

import dev.zucca_ops.kustomtrace.exceptions.InvalidReferenceException;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * A functional interface for strategies that extract and resolve file path references
 * from a given value (typically found within a Kustomization file's parsed content).
 * <p>
 * Implementations of this interface define how to interpret a specific field's value
 * (e.g., a string, a list of strings, or a map) from a Kustomization file and
 * convert it into a stream of resolved {@link Path} objects, relative to a base directory.
 */
@FunctionalInterface
public interface ReferenceExtractor {
    /**
     * Extracts and resolves file paths from a given YAML field value.
     *
     * @param yamlFieldValue The value of the field from the parsed YAML content
     * (e.g., a String, List, or Map).
     * @param baseDir        The base directory against which relative paths in the
     * {@code yamlFieldValue} should be resolved.
     * @return A {@link Stream} of resolved {@link Path} objects representing the
     * extracted file references. Returns an empty stream if no valid
     * references are found or if the input value is not applicable.
     * @throws InvalidReferenceException If the {@code yamlFieldValue} is malformed,
     * contains invalid path strings, or refers to
     * resources that cannot be properly resolved
     * according to the extractor's specific logic.
     */
    Stream<Path> extract(Object yamlFieldValue, Path baseDir) throws InvalidReferenceException;
}