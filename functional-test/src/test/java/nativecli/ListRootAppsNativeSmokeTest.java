package nativecli;

import cli.util.OutputResourceAssesor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ListRootAppsNativeSmokeTest extends NativeCliSmokeTestSupport {

    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("root-apps");

    @TempDir
    Path tempDir;

    @Test
    void testOverallResources() {
        Path actualOutputFile = tempDir.resolve("root-apps-native-output.yaml");
        String expectedResourceFileName = "overall.yaml";

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "list-root-apps"
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testEmptyBecauseCircularApp() {
        Path actualOutputFile = tempDir.resolve("root-apps-native-output.yaml");
        String expectedResourceFileName = "empty.yaml";
        Path appsPath = resourcesDir.resolve("circular-dependency-apps").resolve("app2");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsPath.toString(),
                "--output", actualOutputFile.toString(),
                "list-root-apps"
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }
}
