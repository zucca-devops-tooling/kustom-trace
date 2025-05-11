package exceptions;

import java.nio.file.Path;

public class InvalidContentException extends KustomException {
    public InvalidContentException(Path path) {
        super("File " + path.toString() + " has invalid Kustomize content", path);
    }
}
