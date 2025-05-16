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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReferenceValidators {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceValidators.class);

    public static ReferenceValidator mustBeFile() {
        return path -> {
            logger.debug("Validating if path '{}' is a file.", path);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                logger.warn("Validation failed: Expected a file at '{}'.", path);
                throw new InvalidReferenceException("Expected a file", path);
            }
            logger.trace("Path '{}' is a valid file.", path);
        };
    }

    public static ReferenceValidator mustBeDirectoryWithKustomizationYaml() {
        return path -> {
            logger.debug("Validating if path '{}' is a directory with kustomization.yaml.", path);
            if (!Files.isDirectory(path)) {
                logger.warn("Validation failed: Expected a directory at '{}'.", path);
                throw new InvalidReferenceException("Expected a directory", path);
            }
            Path kustomization = path.resolve("kustomization.yaml");
            if (!Files.exists(kustomization)) {
                logger.warn("Validation failed: Expected a kustomization.yaml inside directory '{}'.", path);
                throw new InvalidReferenceException("Expected a kustomization.yaml inside directory", path);
            }
            logger.trace("Path '{}' is a valid directory with kustomization.yaml.", path);
        };
    }

    public static ReferenceValidator optionalDirectoryOrFile() {
        return path -> {
            logger.debug("Validating if path '{}' exists (optional directory or file).", path);
            if (!Files.exists(path)) {
                logger.warn("Validation failed: Path '{}' does not exist.", path);
                throw new InvalidReferenceException("Path does not exist", path);
            }
            logger.trace("Path '{}' exists.", path);
        };
    }
}