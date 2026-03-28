package nativecli;

import cli.util.OutputResourceAssesor;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppFilesNativeSmokeTest extends NativeCliSmokeTestSupport {

    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("app-files");

    @TempDir
    Path tempDir;

    @Test
    void testConfigmapApp() {
        Path actualOutputFile = tempDir.resolve("app-files-native-output.yaml");
        String expectedResourceFileName = "configmap-files.yaml";
        Path appsPath = resourcesDir.resolve("all-reference-types-apps");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsPath.toString(),
                "--output", actualOutputFile.toString(),
                "app-files", appsPath.resolve("app-configmap").toString()
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testUnparseableApp() {
        Path actualOutputFile = tempDir.resolve("app-files-native-output.yaml");
        String expectedResourceFileName = "app-with-unparseables.yaml";
        Path appsPath = resourcesDir.resolve("app-with-unparseable-kustomization");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsPath.toString(),
                "--output", actualOutputFile.toString(),
                "app-files", appsPath.toString()
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testSubsetWithCircularApp() {
        Path actualOutputFile = tempDir.resolve("app-files-native-output.yaml");
        String expectedResourceFileName = "subset-with-circular.yaml";
        Path appPath = resourcesDir.resolve("complex-apps").resolve("subset-with-circular");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "app-files", appPath.toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(
                result.stdout().contains("Circular dependency detected")
                        || result.stderr().contains("Circular dependency detected"),
                "Expected circular dependency message. stdout: " + result.stdout() + "\nstderr: " + result.stderr());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testNotRootApp() {
        Path actualOutputFile = tempDir.resolve("app-files-native-output.yaml");
        String expectedResourceFileName = "not-root-app.yaml";
        Path appsPath = resourcesDir.resolve("complex-apps").resolve("root-and-all-references");
        Path appPath = resourcesDir.resolve("all-reference-types-apps").resolve("app-base");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsPath.toString(),
                "--output", actualOutputFile.toString(),
                "app-files", appPath.toString()
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }
}
