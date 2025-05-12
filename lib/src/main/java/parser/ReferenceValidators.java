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
package parser;

import exceptions.InvalidReferenceException;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReferenceValidators {

    public static ReferenceValidator mustBeFile() {
        return path -> {
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new InvalidReferenceException("Expected a file", path);
            }
        };
    }

    public static ReferenceValidator mustBeDirectoryWithKustomizationYaml() {
        return path -> {
            if (!Files.isDirectory(path)) {
                throw new InvalidReferenceException("Expected a directory", path);
            }
            Path kustomization = path.resolve("kustomization.yaml");
            if (!Files.exists(kustomization)) {
                throw new InvalidReferenceException("Expected a kustomization.yaml inside directory", path);
            }
        };
    }

    public static ReferenceValidator optionalDirectoryOrFile() {
        return path -> {
            if (!Files.exists(path)) {
                throw new InvalidReferenceException("Path does not exist", path);
            }
        };
    }
}