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

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class YamlParser {

    public static List<Map<String, Object>> parseFile(Path path) throws FileNotFoundException {
        Yaml parser = new Yaml();
        List<Map<String, Object>> documents = new ArrayList<>();

        try (InputStream inputStream = new FileInputStream(path.toAbsolutePath().toString())) {
            Iterable<Object> parsed = parser.loadAll(inputStream);

            for (Object doc : parsed) {
                if (Objects.isNull(doc)) {
                    // TODO: Log null document
                    continue;
                }

                if (doc instanceof Map<?, ?> map) {
                    //noinspection unchecked
                    documents.add((Map<String, Object>) map);
                } else {
                    throw new IllegalArgumentException(
                            "Unsupported YAML document type in file " + path + ": " + doc.getClass().getSimpleName()
                    );
                }
            }

            return documents;

        } catch (IOException e) {
            throw new FileNotFoundException("Could not read YAML file: " + path);
        }
    }

    public static Map<String, Object> parseKustomizationFile(Path path) {
        try {
            List<Map<String, Object>> fileContent = YamlParser.parseFile(path);

            if (fileContent.size() == 1) {
                return fileContent.get(0);
            }

            // TODO: Log incorrect amount of documents in kustomization.yaml
            return null;
        } catch (FileNotFoundException e) {
            //TODO: log kustomization file not found
            return null;
        }
    }
}
