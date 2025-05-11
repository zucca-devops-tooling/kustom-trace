package model;

import java.util.Optional;

public class KustomResource {

    private String name;
    private String kind;

    private KustomFile file;

    public KustomResource() {
    }

    public KustomResource(String name, String kind, KustomFile file) {
        this.name = name;
        this.kind = kind;
        this.file = file;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setFile(KustomFile file) {
        this.file = file;
    }

    public String getDisplayName() {
        return "%s %s %s".formatted(getKind(), getName(), file.getPath().toString());
    }

    public String getName() {
        return Optional.ofNullable(name).orElse("undefined");
    }

    public KustomFile getFile() {
        return file;
    }
}
