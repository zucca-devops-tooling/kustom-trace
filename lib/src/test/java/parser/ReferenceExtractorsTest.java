package parser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class ReferenceExtractorsTest {

    @TempDir
    Path baseDir;
    @Test
    void simpleList_resolvesValidStrings() {
        var extractor = ReferenceExtractors.simpleList();

        List<String> input = List.of("a.yaml", "b.yaml", "multi\nline");
        Stream<Path> result = extractor.extract(input, baseDir);
        List<Path> paths = result.toList();

        assertEquals(2, paths.size());
        assertTrue(paths.contains(baseDir.resolve("a.yaml").normalize()));
        assertTrue(paths.contains(baseDir.resolve("b.yaml").normalize()));
    }

    @Test
    void simpleList_returnsEmptyForNonList() {
        var extractor = ReferenceExtractors.simpleList();
        Stream<Path> result = extractor.extract("not a list", baseDir);
        assertTrue(result.toList().isEmpty());
    }

    @Test
    void inlinePathList_extractsSpecifiedField() {
        var extractor = ReferenceExtractors.inlinePathList("path");

        List<Map<String, Object>> input = List.of(
                Map.of("path", "patch1.yaml"),
                Map.of("path", "patch2.yaml"),
                Map.of("nope", "ignored")
        );

        Stream<Path> result = extractor.extract(input, baseDir);
        List<Path> paths = result.toList();

        assertEquals(2, paths.size());
        assertTrue(paths.contains(baseDir.resolve("patch1.yaml").normalize()));
        assertTrue(paths.contains(baseDir.resolve("patch2.yaml").normalize()));
    }

    @Test
    void configMapGeneratorFiles_extractsEnvsAndFiles() {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();

        List<Map<String, Object>> input = List.of(
                Map.of("files", List.of("a.yaml", "key1=path/to/b.yaml")),
                Map.of("envs", List.of("env1", "env2"))
        );

        Stream<Path> result = extractor.extract(input, baseDir);
        List<Path> paths = result.toList();

        assertEquals(4, paths.size());
        assertTrue(paths.contains(baseDir.resolve("a.yaml").normalize()));
        assertTrue(paths.contains(baseDir.resolve("path/to/b.yaml").normalize()));
        assertTrue(paths.contains(baseDir.resolve("env1").normalize()));
        assertTrue(paths.contains(baseDir.resolve("env2").normalize()));
    }

    @Test
    void configMapGeneratorFiles_returnsEmptyForNonList() {
        var extractor = ReferenceExtractors.configMapGeneratorFiles();
        Stream<Path> result = extractor.extract("not a list", baseDir);
        assertTrue(result.toList().isEmpty());
    }
}
