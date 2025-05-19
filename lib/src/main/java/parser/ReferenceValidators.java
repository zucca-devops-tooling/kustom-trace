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
                throw new InvalidReferenceException("Expected a file", path);
            }
            logger.trace("Path '{}' is a valid file.", path);
        };
    }

    public static ReferenceValidator mustBeKubernetesResource() {
        return path -> {
            logger.debug("Validating if path '{}' is a Kubernetes resource file or directory.", path);
            if (Files.isDirectory(path)) {
                logger.trace("Path '{}' is a directory.", path);
                return; // Directories are valid for resources
            }
            if (Files.isRegularFile(path)) {
                if (YamlParser.isValidKubernetesResource(path)) {
                    logger.trace("Path '{}' is a valid Kubernetes resource file.", path);
                    return;
                } else {
                    throw new InvalidReferenceException("Expected a .yaml, .yml, or .json file", path);
                }
            }
            throw new InvalidReferenceException("Expected a directory or a .yaml, .yml, or .json file", path);
        };
    }

    public static ReferenceValidator mustBeDirectoryOrKustomizationFile() {
        return path -> {
            logger.debug("Validating if path '{}' is a directory with kustomization or a kustomization file.", path);
            if (Files.isDirectory(path)) {
                Path kustomizationYaml = path.resolve("kustomization.yaml");
                Path kustomizationYml = path.resolve("kustomization.yml");
                if (!Files.exists(kustomizationYaml) && !Files.exists(kustomizationYml)) {
                    throw new InvalidReferenceException("Expected a kustomization.yaml or kustomization.yml inside directory", path);
                }
                logger.trace("Path '{}' is a valid directory containing a kustomization file.", path);
                return;
            }
            if (Files.isRegularFile(path)) {
                if (YamlParser.isValidKustomizationFile(path)) {
                    logger.trace("Path '{}' is a valid kustomization file.", path);
                    return;
                } else {
                    throw new InvalidReferenceException("Expected a kustomization.yaml/yml file", path);
                }
            }
            throw new InvalidReferenceException("Expected a directory or a kustomization.yaml/yml file", path);
        };
    }
}