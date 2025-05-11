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
        if (parseableExtensions.anyMatch(path::endsWith)) {
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
