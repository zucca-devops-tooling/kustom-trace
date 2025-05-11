package parser;

import exceptions.InvalidReferenceException;

import java.nio.file.Path;

@FunctionalInterface
public interface ReferenceValidator {
    void validate(Path path) throws InvalidReferenceException;
}
