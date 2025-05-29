package cli.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class OutputResourceAssesor {

    private final Path resourcesDir = Paths.get( "src", "test", "resources", "cli-expected-outputs");
    private final String testDir;
    private final Yaml yaml = new Yaml();

    public OutputResourceAssesor(String testDir) {
        this.testDir = testDir;
    }

    /**
     * Loads YAML from a test resource file into a Map.
     * @param resourceFile The filename for the particular assessment (e.g., "data.yaml")
     * @return A Map representing the parsed YAML.
     */
    private Map<String, Object> loadExpectedYaml(String resourceFile) {
        Path resourcePath = resourcesDir.resolve(testDir).resolve(resourceFile);
        try (Reader reader = Files.newBufferedReader(resourcePath, StandardCharsets.UTF_8)) {
            return yaml.load(reader);
        } catch (IOException e) {
            fail("Cannot find the expected YAML resource file: " + resourcePath);
            return null;
        }
    }

    /**
     * Loads YAML from an actual output file (Path) into a Map.
     * @param actualFilePath Path to the CLI-generated output file.
     * @return A Map representing the parsed YAML.
     */
    private Map<String, Object> loadActualYaml(Path actualFilePath) {
        try (Reader reader = Files.newBufferedReader(actualFilePath, StandardCharsets.UTF_8)) {
            return yaml.load(reader);
        } catch (IOException e) {
            fail("Cannot find the expected YAML resource file: " + actualFilePath);
            return null;
        }
    }

    /**
     * Asserts that the YAML content of an actual output file matches the YAML content
     * of an expected resource file.
     *
     * @param actualOutputFile Path to the CLI-generated output file.
     * @param expectedResourceFile The path to the expected YAML file within src/test/resources.
     */
    public void assertYamlOutputMatchesResource(Path actualOutputFile, String expectedResourceFile) {
        Map<String, Object> expectedData = loadExpectedYaml(expectedResourceFile);
        Map<String, Object> actualData = loadActualYaml(actualOutputFile);

        assertNotNull(expectedData, "Expected YAML data from resource should not be null.");
        assertNotNull(actualData, "Actual YAML data from output file should not be null.");

        assertEquals(expectedData, actualData,
                "The YAML content from file " + actualOutputFile +
                        " does not match the expected content from resource " + expectedResourceFile);
    }
}
