package exceptions;

import java.nio.file.Path;

public class InvalidReferenceException extends KustomException {
    public InvalidReferenceException(String message, Path path) {
        super(message, path);
    }
}
