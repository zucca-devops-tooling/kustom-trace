package nativecli;

import cli.util.OutputResourceAssesor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CLIBasicNativeSmokeTest extends NativeCliSmokeTestSupport {

    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("basic-test");

    private String readFileContent(Path filePath) throws IOException {
        return Files.readString(filePath).trim();
    }

    private void createSimpleKustomization(Path kustomizationPath, List<String> resources) throws IOException {
        Files.createDirectories(kustomizationPath.getParent());
        StringBuilder content = new StringBuilder();
        content.append("apiVersion: kustomize.config.k8s.io/v1beta1\n");
        content.append("kind: Kustomization\n");
        if (resources != null && !resources.isEmpty()) {
            content.append("resources:\n");
            for (String resource : resources) {
                content.append("  - ").append(resource).append("\n");
            }
        }
        Files.writeString(kustomizationPath, content.toString());
    }

    @Test
    void testErrorRedirectionAppFilesMissingAppPathNoErrorFileSpecified(@TempDir Path tempDir)
            throws IOException {
        Path appsActualDir = Files.createDirectory(tempDir.resolve("apps"));

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsActualDir.toString(),
                "app-files"
        );

        assertEquals(2, result.exitCode());
        assertTrue(result.stderr().contains("Missing required parameter: '<app-path>'"));
        assertTrue(result.stderr().contains("Usage: kustomtrace app-files"));
    }

    @Test
    void testErrorRedirectionInvalidAppPathFileGoesToLogFile(@TempDir Path tempDir) throws IOException {
        Path appsActualDir = Files.createDirectory(tempDir.resolve("apps"));
        Path designatedLogFile = tempDir.resolve("error_invalid_app_path.log");
        Path nonExistentAppFile = appsActualDir.resolve("non_existent_kustomization.yaml");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsActualDir.toString(),
                "--log-file", designatedLogFile.toString(),
                "app-files",
                nonExistentAppFile.toString()
        );

        assertEquals(1, result.exitCode());
        assertTrue(Files.exists(designatedLogFile));
        String logFileContent = readFileContent(designatedLogFile);

        String expectedFullErrorMessage =
                "Error: Invalid <app-path> (path does not exist): " + nonExistentAppFile.toAbsolutePath();

        assertEquals(expectedFullErrorMessage, logFileContent);
        assertTrue(result.stderr().isEmpty(), "stderr should be empty when error went to the log file.");
    }

    @Test
    void testAppFilesCommandOutputToFileWritesYaml(@TempDir Path tempDir) throws IOException {
        Path appsDir = Files.createDirectory(tempDir.resolve("apps"));
        Path app1Dir = Files.createDirectory(appsDir.resolve("app1"));
        Path baseDir = Files.createDirectory(appsDir.resolve("base"));
        Path kustomizationApp1Path = app1Dir.resolve("kustomization.yaml");
        Path baseYamlPath = baseDir.resolve("base-resource.yaml");
        Path app1OverlayResourcePath = app1Dir.resolve("app1-specific.yaml");
        Files.createFile(baseYamlPath);
        Files.createFile(app1OverlayResourcePath);
        createSimpleKustomization(
                kustomizationApp1Path,
                Arrays.asList("../base/base-resource.yaml", "app1-specific.yaml"));

        Path actualOutputFile = tempDir.resolve("app-files-output.yaml");
        String expectedResourceFile = "app-files.yaml";

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsDir.toString(),
                "--output", actualOutputFile.toString(),
                "app-files", app1Dir.toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stderr().isEmpty(), "stderr should be empty on success.");
        assertTrue(Files.exists(actualOutputFile));
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFile);

        assertFalse(result.stdout().contains("Files used by application at:"));
    }

    @Test
    void testAffectedAppsCommandOutputToFileWritesYaml(@TempDir Path tempDir) throws IOException {
        Path appsDir = Files.createDirectory(tempDir.resolve("apps"));
        Path app1Dir = Files.createDirectory(appsDir.resolve("app1"));
        Path app2Dir = Files.createDirectory(appsDir.resolve("app2"));
        Path baseDir = Files.createDirectory(appsDir.resolve("base"));

        Path kustomizationApp1Path = app1Dir.resolve("kustomization.yaml");
        Path kustomizationApp2Path = app2Dir.resolve("kustomization.yaml");
        Path commonBaseYamlPath = baseDir.resolve("common-base.yaml");
        Path app2SpecificResourcePath = app2Dir.resolve("app2-specific.yaml");

        Files.createFile(commonBaseYamlPath);
        createSimpleKustomization(kustomizationApp1Path, List.of("../base/common-base.yaml"));
        createSimpleKustomization(kustomizationApp2Path, List.of("../base/common-base.yaml", "app2-specific.yaml"));
        if (!Files.exists(app2SpecificResourcePath)) {
            Files.createFile(app2SpecificResourcePath);
        }

        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "affected-apps.yaml";

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps", commonBaseYamlPath.toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stderr().isEmpty(), "stderr should be empty on success.");
        assertTrue(Files.exists(actualOutputFile));
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);

        assertFalse(result.stdout().contains("Affected Applications:"));
        assertFalse(result.stdout().contains("Affected apps by"));
        assertFalse(result.stdout().contains("  - "));
        assertFalse(result.stdout().contains("Output written to:"));
    }

    @Test
    void testMissingAppsDirGlobal() {
        NativeCliResult result = nativeCliExecutor.execute("app-files", "some/app/path.yaml");

        assertNotEquals(0, result.exitCode());
        assertTrue(result.stderr().contains("Missing required option: '--apps-dir=<appsDir>'"));
    }

    @Test
    void testInvalidAppsDirNonExistent(@TempDir Path tempDir) throws IOException {
        Path nonExistentDir = tempDir.resolve("nonexistent123");
        Path dummyAppDir = Files.createDirectory(tempDir.resolve("dummyApp"));
        Path dummyAppPath = Files.createFile(dummyAppDir.resolve("kustomization.yaml"));

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", nonExistentDir.toString(),
                "app-files", dummyAppPath.toString()
        );

        assertNotEquals(0, result.exitCode());
        assertTrue(
                result.stderr().contains(
                        "Error: Invalid --apps-dir (not a directory or does not exist): " + nonExistentDir));
    }

    @Test
    void testInvalidAppsDirNotADirectory(@TempDir Path tempDir) throws IOException {
        Path notADirectory = Files.createFile(tempDir.resolve("iamajustfile.txt"));
        Path dummyAppDir = Files.createDirectory(tempDir.resolve("dummyApp"));
        Path dummyAppPath = Files.createFile(dummyAppDir.resolve("kustomization.yaml"));

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", notADirectory.toString(),
                "app-files", dummyAppPath.toString()
        );

        assertNotEquals(0, result.exitCode());
        assertTrue(
                result.stderr().contains(
                        "Error: Invalid --apps-dir (not a directory or does not exist): " + notADirectory));
    }

    @Test
    void testEmptyAppsDirAffectedAppsCommandWritesYaml(@TempDir Path tempDir) {
        Path emptyAppsDir = resourcesDir.resolve("app-with-no-kustomization");
        Path modifiedFile = emptyAppsDir.resolve("deployment.yml");

        Path actualOutputFile = tempDir.resolve("empty-apps-affected-actual-output.yaml");
        String expectedResourceFileName = "unreferenced-file.yaml";

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", emptyAppsDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                modifiedFile.toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stderr().isEmpty(), "stderr should be empty when warnings are redirected or suppressed.");
        assertTrue(Files.exists(actualOutputFile));
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);

        assertFalse(result.stdout().contains("Affected Applications:"));
        assertFalse(result.stdout().contains("Affected apps by"));
        assertFalse(result.stdout().contains("  - "));
        assertFalse(result.stdout().contains("Output written to:"));
    }
}
