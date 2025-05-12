package parser;

import exceptions.InvalidReferenceException;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum ReferenceType {

    RESOURCE("resources", ReferenceExtractors.simpleList(), ReferenceValidators.optionalDirectoryOrFile()),
    BASE("bases", ReferenceExtractors.simpleList(), ReferenceValidators.optionalDirectoryOrFile()),
    COMPONENT("components", ReferenceExtractors.simpleList(), ReferenceValidators.mustBeDirectoryWithKustomizationYaml()),
    PATCH("patches", ReferenceExtractors.inlinePathList("path"), ReferenceValidators.mustBeFile()),
    PATCH_MERGE("patchesStrategicMerge", ReferenceExtractors.simpleList(), ReferenceValidators.mustBeFile()),
    CONFIG_MAP("configMapGenerator", ReferenceExtractors.configMapGeneratorFiles(), ReferenceValidators.mustBeFile());

    private final String yamlKey;
    private final ReferenceExtractor extractor;
    private final ReferenceValidator validator;

    ReferenceType(String yamlKey, ReferenceExtractor extractor, ReferenceValidator validator) {
        this.yamlKey = yamlKey;
        this.extractor = extractor;
        this.validator = validator;
    }

    public String getYamlKey() {
        return yamlKey;
    }

    public Stream<Path> extract(Object yamlValue, Path baseDir) {
        Object value = ((Map<String, Object>)yamlValue).get(this.yamlKey);
        return extractor.extract(value, baseDir).peek(System.out::println);
    }

    public void validate(Path path) throws InvalidReferenceException {
        validator.validate(path);
    }

    // Static lookup map for YAML key → enum
    private static final Map<String, ReferenceType> byKey = Arrays.stream(values())
            .collect(Collectors.toMap(ReferenceType::getYamlKey, Function.identity()));

    public static ReferenceType fromYamlKey(String key) {
        return byKey.get(key);
    }

    public static Boolean isReference(String key) {
        return byKey.containsKey(key);
    }
}