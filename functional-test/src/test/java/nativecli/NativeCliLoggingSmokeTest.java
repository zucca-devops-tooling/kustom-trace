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

    private final OutputResourceAssesor appFilesOutputResourceAssesor =
            new OutputResourceAssesor("app-files");
    private final OutputResourceAssesor rootAppsOutputResourceAssesor =
            new OutputResourceAssesor("root-apps");

    @TempDir
    Path tempDir;

    @Test
    void testListRootAppsWritesResolverWarningsToLogFile() throws IOException {
        Path actualOutputFile = tempDir.resolve("root-apps-native-output.yaml");
        Path logFile = tempDir.resolve("list-root-apps-native.log");

        NativeCliResult result =
                nativeCliExecutor.execute(
                        "--apps-dir",
                        resourcesDir.toString(),
                        "--log-file",
                        logFile.toString(),
                        "--output",
                        actualOutputFile.toString(),
                        "list-root-apps");

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(logFile));

        String logFileContent = Files.readString(logFile);
        assertTrue(logFileContent.contains("Error parsing content for dependency type 'BASE'"));
        assertFalse(logFileContent.contains("Building Kustomization for:"));
        assertTrue(result.stderr().isEmpty(), "stderr should be empty when diagnostics go to the log file.");
        assertFalse(
                result.stdout().contains("Error parsing content for dependency type 'BASE'"),
                "stdout should not carry resolver diagnostics when --log-file is set.");

        rootAppsOutputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, "overall.yaml");
    }

    @Test
    void testAppFilesWritesCircularDependencyDiagnosticsToLogFile() throws IOException {
        Path actualOutputFile = tempDir.resolve("app-files-native-output.yaml");
        Path logFile = tempDir.resolve("app-files-native.log");
        Path appPath = resourcesDir.resolve("complex-apps").resolve("subset-with-circular");

        NativeCliResult result =
                nativeCliExecutor.execute(
                        "--apps-dir",
                        resourcesDir.toString(),
                        "--log-file",
                        logFile.toString(),
                        "--output",
                        actualOutputFile.toString(),
                        "app-files",
                        appPath.toString());

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(logFile));

        String logFileContent = Files.readString(logFile);
        assertTrue(logFileContent.contains("Circular dependency detected"));
        assertTrue(result.stderr().isEmpty(), "stderr should be empty when diagnostics go to the log file.");
        assertFalse(
                result.stdout().contains("Circular dependency detected"),
                "stdout should not carry circular dependency diagnostics when --log-file is set.");

        appFilesOutputResourceAssesor.assertYamlOutputMatchesResource(
                actualOutputFile, "subset-with-circular.yaml");
    }

    @Test
    void testDebugLogLevelEnablesDebugEntriesInLogFile() throws IOException {
        Path actualOutputFile = tempDir.resolve("root-apps-native-output.yaml");
        Path logFile = tempDir.resolve("list-root-apps-native-debug.log");

        NativeCliResult result =
                nativeCliExecutor.execute(
                        "--apps-dir",
                        resourcesDir.toString(),
                        "--log-file",
                        logFile.toString(),
                        "--log-level",
                        "DEBUG",
                        "--output",
                        actualOutputFile.toString(),
                        "list-root-apps");

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(logFile));

        String logFileContent = Files.readString(logFile);
        assertTrue(logFileContent.contains("Building Kustomization for:"));
        assertTrue(result.stderr().isEmpty(), "stderr should be empty when diagnostics go to the log file.");

        rootAppsOutputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, "overall.yaml");
    }
}
