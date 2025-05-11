package exceptions;

import java.nio.file.Path;

public class NotAnAppException extends KustomException {

    public NotAnAppException(Path path) {
        super("File with path " + path.toString() + " is not a root kustomization yaml", path);
        this.path = path;
    }
}
