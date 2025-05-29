package cli;

import cli.util.OutputResourceAssesor;
import cli.util.WriterOutputStream;
import dev.zucca_ops.kustomtrace.cli.KustomTraceCLI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CLIBasicTest {

    private CommandLine cmd;
    private StringWriter swOut;
    private StringWriter swErr;

    private PrintStream originalOut;
    private PrintStream originalErr;
    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("basic-test");

    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalErr = System.err;

        swOut = new StringWriter();
        System.setOut(new PrintStream(new WriterOutputStream(swOut, StandardCharsets.UTF_8.name()), true));

        swErr = new StringWriter();
        System.setErr(new PrintStream(new WriterOutputStream(swErr, StandardCharsets.UTF_8.name()), true));

        cmd = new CommandLine(new KustomTraceCLI()); // Instantiate your actual KustomTraceCLI
        System.clearProperty("log.file");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.clearProperty("log.file");
    }

    private String getCapturedOut() { return swOut.toString().trim(); }
    private String getCapturedErr() { return swErr.toString().trim(); }
    private String readFileContent(Path filePath) throws IOException { return Files.readString(filePath).trim(); }

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
    void testErrorRedirection_AppFilesMissingAppPath_NoErrorFileSpecified(@TempDir Path tempDir) throws IOException {
        Path appsActualDir = Files.createDirectory(tempDir.resolve("apps"));

        int exitCode = cmd.execute(
                "--apps-dir", appsActualDir.toString(),
                "app-files" // Missing <app-path>
        );

        // Picocli exits with 2 for missing required parameters detected by the framework
        assertEquals(2, exitCode, "Exit code should be 2 (Picocli usage error)");
        String errContent = getCapturedErr();

        // Assert that Picocli's standard error message for missing parameters is present
        // The exact parameter name in the message might be '<app-path>' or the field name.
        assertTrue(errContent.contains("Missing required parameter: '<app-path>'"), // Or similar, based on Picocli's output
                "System.err should contain Picocli's missing parameter error. Actual: " + errContent);
        assertTrue(errContent.contains("Usage: kustomtrace app-files"),
                "System.err should contain usage information. Actual: " + errContent);
    }

    @Test
    void testErrorRedirection_InvalidAppPathFile_GoesToLogFile(@TempDir Path tempDir) throws IOException {
        Path appsActualDir = Files.createDirectory(tempDir.resolve("apps"));
        Path designatedLogFile = tempDir.resolve("error_invalid_app_path.log");
        Path nonExistentAppFile = appsActualDir.resolve("non_existent_kustomization.yaml");

        int exitCode = cmd.execute(
                "--apps-dir", appsActualDir.toString(),
                "--log-file", designatedLogFile.toString(),
                "app-files",
                nonExistentAppFile.toString()
        );

        assertEquals(1, exitCode, "Exit code should be 1 (custom command error for invalid app path)");
        assertTrue(Files.exists(designatedLogFile), "Designated log file should be created.");
        String logFileContent = readFileContent(designatedLogFile);

        String expectedFullErrorMessage = "Error: Invalid <app-path> (path does not exist): " + nonExistentAppFile.toAbsolutePath();

        assertEquals(logFileContent, expectedFullErrorMessage, "Log file should contain the exact invalid app path error. " +
                "\nExpected to find: '" + expectedFullErrorMessage + "'" +
                "\nActual:           '" + logFileContent + "'");

        assertTrue(getCapturedErr().isEmpty(),
                "System.err should be empty if error went to the log file. Actual stderr: " + getCapturedErr());
    }

    @Test
    void testAppFilesCommand_OutputToFile_WritesYaml(@TempDir Path tempDir) throws IOException {
        // ... (setup code for appsDir, kustomizationApp1Path, etc. is the same) ...
        Path appsDir = Files.createDirectory(tempDir.resolve("apps"));
        Path app1Dir = Files.createDirectory(appsDir.resolve("app1"));
        Path baseDir = Files.createDirectory(appsDir.resolve("base"));
        Path kustomizationApp1Path = app1Dir.resolve("kustomization.yaml");
        Path baseYamlPath = baseDir.resolve("base-resource.yaml");
        Path app1OverlayResourcePath = app1Dir.resolve("app1-specific.yaml");
        Files.createFile(baseYamlPath);
        Files.createFile(app1OverlayResourcePath);
        createSimpleKustomization(kustomizationApp1Path, Arrays.asList("../base/base-resource.yaml", "app1-specific.yaml"));


        Path actualOutputFile = tempDir.resolve("app-files-output.yaml");
        String expectedResourceFile = "app-files.yaml";

        // Execute the command
        int exitCode = cmd.execute(
                "--apps-dir", appsDir.toString(),
                "--output", actualOutputFile.toString(),
                "app-files", app1Dir.toString()
        );

        assertEquals(0, exitCode, "Exit code should be 0. Stderr: " + getCapturedErr());
        assertTrue(getCapturedErr().isEmpty(), "Stderr should be empty on success. Actual: " + getCapturedErr());
        assertTrue(Files.exists(actualOutputFile), "Output YAML file should be created.");

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFile);

        // System.out assertions (should be clean of command data)
        String capturedStdOut = getCapturedOut();
        assertFalse(capturedStdOut.contains("Files used by application at:"), "System.out should NOT contain verbose output header.");
    }

    @Test
    void testAffectedAppsCommand_OutputToFile_WritesYaml(@TempDir Path tempDir) throws IOException {
        // 1. Setup
        Path appsDir = Files.createDirectory(tempDir.resolve("apps"));
        Path app1Dir = Files.createDirectory(appsDir.resolve("app1"));
        Path app2Dir = Files.createDirectory(appsDir.resolve("app2"));
        Path baseDir = Files.createDirectory(appsDir.resolve("base"));

        Path kustomizationApp1Path = app1Dir.resolve("kustomization.yaml");
        Path kustomizationApp2Path = app2Dir.resolve("kustomization.yaml");
        Path commonBaseYamlPath = baseDir.resolve("common-base.yaml");
        Path app2SpecificResourcePath = app2Dir.resolve("app2-specific.yaml"); // For clarity

        Files.createFile(commonBaseYamlPath);
        createSimpleKustomization(kustomizationApp1Path, List.of("../base/common-base.yaml"));
        createSimpleKustomization(kustomizationApp2Path, List.of("../base/common-base.yaml", "app2-specific.yaml"));
        if (!Files.exists(app2SpecificResourcePath)) {
            Files.createFile(app2SpecificResourcePath);
        }

        // 2. Define actual output file path and expected resource file name
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "affected-apps.yaml"; // The file you created in resources

        // 3. Execute command
        int exitCode = cmd.execute(
                "--apps-dir", appsDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps", commonBaseYamlPath.toString()
        );

        // 4. Basic Assertions
        assertEquals(0, exitCode, "Exit code should be 0. Stderr: " + getCapturedErr());
        assertTrue(getCapturedErr().isEmpty(), "Stderr should be empty on success. Actual: " + getCapturedErr());
        assertTrue(Files.exists(actualOutputFile), "Output YAML file should be created.");

        // 5. Use OutputResourceAssessor to compare the YAML content
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);

        // 6. System.out assertions (should be clean of the command's data output)
        String capturedStdOut = getCapturedOut();
        assertFalse(capturedStdOut.contains("Affected Applications:"), "System.out should NOT contain verbose 'Affected Applications:' header.");
        assertFalse(capturedStdOut.contains("Affected apps by"), "System.out should NOT contain verbose 'Affected apps by' sub-headers.");
        assertFalse(capturedStdOut.contains("  - "), "System.out should NOT contain verbose item listing ('  - ').");
        assertFalse(capturedStdOut.contains("Output written to:"), "System.out should NOT contain 'Output written to:' message.");
    }

    @Test
    void testMissingAppsDir_Global() {
        int exitCode = cmd.execute("app-files", "some/app/path.yaml");
        assertNotEquals(0, exitCode);
        assertTrue(getCapturedErr().contains("Missing required option: '--apps-dir=<appsDir>'"),
                "Error messages missmatch\n" +
                        "Expected: Missing required option: '--apps-dir=<appsDir>'\n" +
                        "Actual: " + getCapturedErr());
    }

    @Test
    void testInvalidAppsDir_NonExistent(@TempDir Path tempDir) throws IOException {
        Path nonExistentDir = tempDir.resolve("nonexistent123");
        Path dummyAppDir = Files.createDirectory(tempDir.resolve("dummyApp"));
        Path dummyAppPath = Files.createFile(dummyAppDir.resolve("kustomization.yaml"));

        int exitCode = cmd.execute(
                "--apps-dir", nonExistentDir.toString(),
                "app-files", dummyAppPath.toString()
        );
        assertNotEquals(0, exitCode);
        assertTrue(getCapturedErr().contains("Error: Invalid --apps-dir (not a directory or does not exist): " + nonExistentDir),
                "Custom error for non-existent --apps-dir value expected. Actual stderr: " + getCapturedErr());
    }

    @Test
    void testInvalidAppsDir_NotADirectory(@TempDir Path tempDir) throws IOException {
        Path notADirectory = Files.createFile(tempDir.resolve("iamajustfile.txt"));
        Path dummyAppDir = Files.createDirectory(tempDir.resolve("dummyApp"));
        Path dummyAppPath = Files.createFile(dummyAppDir.resolve("kustomization.yaml"));

        int exitCode = cmd.execute(
                "--apps-dir", notADirectory.toString(),
                "app-files", dummyAppPath.toString()
        );
        assertNotEquals(0, exitCode);
        assertTrue(getCapturedErr().contains("Error: Invalid --apps-dir (not a directory or does not exist): " + notADirectory),
                "Custom error for non-directory --apps-dir value expected. Actual stderr: " + getCapturedErr());
    }

    @Test
    void testEmptyAppsDir_AffectedAppsCommand_WritesYaml(@TempDir Path tempDir) {
        Path emptyAppsDir = Paths.get( "src", "test", "resources", "app-with-no-kustomization");
        Path modifiedFile = emptyAppsDir.resolve("deployment.yml");

        Path actualOutputFile = tempDir.resolve("empty-apps-affected-actual-output.yaml");
        String expectedResourceFileName = "unreferenced-file.yaml";

        int exitCode = cmd.execute(
                "--apps-dir", emptyAppsDir.toString(), // Pointing to an empty directory
                "--output", actualOutputFile.toString(),
                "affected-apps",
                modifiedFile.toString()
        );

        // 1. Basic Assertions
        assertEquals(0, exitCode, "Exit code should be 0. Stderr: " + getCapturedErr());
        assertTrue(getCapturedErr().isEmpty(), "Stderr should be empty if warnings go to log file or are suppressed. Actual: " + getCapturedErr());
        assertTrue(Files.exists(actualOutputFile), "Output YAML file should be created.");

        // 2. Use OutputResourceAssessor to compare the YAML content
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);

        // 3. System.out assertions (should be clean of command data and specific messages)
        String capturedStdOut = getCapturedOut();
        assertFalse(capturedStdOut.contains("Affected Applications:"), "System.out should NOT contain verbose output header.");
        assertFalse(capturedStdOut.contains("Affected apps by"), "System.out should NOT contain verbose sub-headers.");
        assertFalse(capturedStdOut.contains("  - "), "System.out should NOT contain verbose item listing ('  - ').");
        assertFalse(capturedStdOut.contains("Output written to:"), "System.out should NOT contain 'Output written to:' message.");
    }
}
