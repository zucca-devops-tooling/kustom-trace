package exceptions;

import java.nio.file.Path;

public class UnreferencedFileException extends KustomException {

    public UnreferencedFileException(Path path) {
        super("File with path " + path.toString() + " is not referenced by any app", path);
        this.path = path;
    }


}
