package parser;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@FunctionalInterface
public interface ReferenceExtractor {
    Stream<Path> extract(Object yamlFieldValue, Path baseDir);
}