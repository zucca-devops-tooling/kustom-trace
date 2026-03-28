package nativecli;

import cli.util.OutputResourceAssesor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeCliLoggingSmokeTest extends NativeCliSmokeTestSupport {

    private final OutputResourceAssesor basicTestOutputResourceAssesor =
            new OutputResourceAssesor("basic-test");

    @TempDir
    Path tempDir;

    @Test
    void testAffectedAppsWritesUnreferencedFileWarningsToLogFile() throws IOException {
        Path emptyAppsDir = resourcesDir.resolve("app-with-no-kustomization");
        Path modifiedFile = emptyAppsDir.resolve("deployment.yml");
        Path actualOutputFile = tempDir.resolve("empty-apps-affected-native-output.yaml");
        Path logFile = tempDir.resolve("affected-apps-native.log");

        NativeCliResult result =
                nativeCliExecutor.execute(
                        "--apps-dir",
                        emptyAppsDir.toString(),
                        "--log-file",
                        logFile.toString(),
                        "--output",
                        actualOutputFile.toString(),
                        "affected-apps",
                        modifiedFile.toString());

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(logFile));

        String logFileContent = Files.readString(logFile);
        assertTrue(logFileContent.contains("Warning: File with path"));
        assertTrue(logFileContent.contains("is not referenced by any app"));
        assertTrue(result.stderr().isEmpty(), "stderr should be empty when warnings go to the log file.");
        assertFalse(
                result.stdout().contains("Warning:"),
                "stdout should not carry unreferenced file warnings when --log-file is set.");

        basicTestOutputResourceAssesor.assertYamlOutputMatchesResource(
                actualOutputFile, "unreferenced-file.yaml");
    }

    @Test
    void testLogLevelOptionDoesNotBreakDedicatedLogFileWarnings() throws IOException {
        Path emptyAppsDir = resourcesDir.resolve("app-with-no-kustomization");
        Path modifiedFile = emptyAppsDir.resolve("deployment.yml");
        Path actualOutputFile = tempDir.resolve("empty-apps-affected-native-output.yaml");
        Path logFile = tempDir.resolve("affected-apps-native-debug.log");

        NativeCliResult result =
                nativeCliExecutor.execute(
                        "--apps-dir",
                        emptyAppsDir.toString(),
                        "--log-file",
                        logFile.toString(),
                        "--log-level",
                        "DEBUG",
                        "--output",
                        actualOutputFile.toString(),
                        "affected-apps",
                        modifiedFile.toString());

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(logFile));

        String logFileContent = Files.readString(logFile);
        assertTrue(logFileContent.contains("Warning: File with path"));
        assertTrue(logFileContent.contains("is not referenced by any app"));
        assertTrue(
                result.stderr().isEmpty(),
                "stderr should remain empty when warnings go to the dedicated log file.");

        basicTestOutputResourceAssesor.assertYamlOutputMatchesResource(
                actualOutputFile, "unreferenced-file.yaml");
    }
}
