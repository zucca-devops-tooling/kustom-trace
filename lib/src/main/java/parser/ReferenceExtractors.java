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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class ReferenceExtractors {

    public static ReferenceExtractor simpleList() {
        return (value, baseDir) -> {
            if (!(value instanceof List<?> references)) return Stream.empty();

            return references.stream()
                    .filter(String.class::isInstance)
                    .map(Object::toString)
                    .filter(ref -> !ref.contains("\n"))
                    .map(ref -> baseDir.resolve(ref).normalize());
        };
    }

    public static ReferenceExtractor inlinePathList(String pathField) {
        return (value, baseDir) -> {
            if (value instanceof List<?> list) {
                return list.stream()
                        .filter(Map.class::isInstance)
                        .map(Map.class::cast)
                        .map(m -> m.get(pathField))
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .map(path -> baseDir.resolve(path).normalize());
            }
            return Stream.empty();
        };
    }

    public static ReferenceExtractor configMapGeneratorFiles() {
        return (value, baseDir) -> {
            if (!(value instanceof List<?> references)) return Stream.empty();

            return references.stream()
                    .filter(Map.class::isInstance)
                    .map(ref -> (Map<String, Object>) ref)
                    .flatMap(ref -> {
                        Stream<String> envs = ref.containsKey("envs")
                                ? ((List<String>) ref.get("envs")).stream()
                                : Stream.empty();

                        Stream<String> files = ref.containsKey("files")
                                ? ((List<String>) ref.get("files")).stream()
                                .map(f -> f.contains("=") ? f.substring(f.indexOf("=") + 1) : f)
                                : Stream.empty();

                        return Stream.concat(envs, files);
                    })
                    .map(f -> baseDir.resolve(f).normalize());
        };
    }
}