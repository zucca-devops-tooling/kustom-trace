package exceptions;

import java.nio.file.Path;

public abstract class KustomException extends Exception {
    protected Path path;

    public KustomException(String message, Path path) {
        super(message);
        this.path = path;
    }
}
