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
package graph;

import exceptions.InvalidContentException;
import model.KustomFile;
import model.KustomResource;
import model.Kustomization;
import parser.YamlParser;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class GraphNodeResolver {

    static KustomFile resolveKustomFile(Path path) {
        Stream<String> parseableExtensions = Stream.of(".yml", ".yaml", ".json");

        KustomFile file = new KustomFile(path);
        String fileName = path.getFileName().toString();

        if (parseableExtensions.anyMatch(fileName::endsWith)) {
            try {
                YamlParser.parseFile(path)
                        .stream()
                        .map(GraphNodeResolver::resolveResource)
                        .forEach(resource -> {
                            file.addResource(resource);
                            resource.setFile(file);
                        });
            } catch (FileNotFoundException e) {
                // TODO: Log corrupted file
            }
        }

        return file;
    }

    static KustomResource resolveResource(Map<String, Object> document) {
        KustomResource resource = new KustomResource();

        if (document.containsKey("kind")) {
            resource.setKind((String) document.get("kind"));
        }

        if (document.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) document.get("metadata");
            if (metadata.containsKey("name")) {
                resource.setName((String) metadata.get("name"));
            }
        }

        return resource;
    }

    static Kustomization resolveKustomization(Path path) throws InvalidContentException {
        Map<String, Object> fileContent = YamlParser.parseKustomizationFile(path);

        if (fileContent == null) {
            throw new InvalidContentException(path);
        }

        return new Kustomization(path, fileContent);
    }
}
