package cli;

import cli.util.OutputResourceAssesor;
import cli.util.WriterOutputStream;
import dev.zucca_ops.kustomtrace.cli.KustomTraceCLI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AffectedAppsTest {

    private final Path resourcesDir = Paths.get( "src", "test", "resources");
    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("affected-apps");
    private CommandLine cmd;
    private StringWriter swOut;
    private StringWriter swErr;

    private PrintStream originalOut;
    private PrintStream originalErr;


    private String getCapturedOut() { return swOut.toString().trim(); }
    private String getCapturedErr() { return swErr.toString().trim(); }

    @TempDir
    Path tempDir;
    @BeforeEach
    void setUp() {
        originalOut = System.out;
        originalErr = System.err;

        swOut = new StringWriter();
        System.setOut(new PrintStream(new WriterOutputStream(swOut, StandardCharsets.UTF_8.name()), true));

        swErr = new StringWriter();
        System.setErr(new PrintStream(new WriterOutputStream(swErr, StandardCharsets.UTF_8.name()), true));

        cmd = new CommandLine(new KustomTraceCLI());
        System.clearProperty("log.file");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.clearProperty("log.file");
    }
    @Test
    void testCircularDependency() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "circular-dependency.yaml"; // The file you created in resources
        Path circularDependencyAppsPath = resourcesDir.resolve("circular-dependency-apps");
        int exitCode = cmd.execute(
                "--apps-dir", circularDependencyAppsPath.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps", circularDependencyAppsPath.resolve("app1").resolve("app1-resource.yaml").toString()
        );

        assertTrue(getCapturedOut().contains("Circular dependency detected"),
                "Error messages missmatch\n" +
                        "Circular dependency detected \n" +
                        "Actual: " + getCapturedOut());
        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testAllApps() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "all-apps.yaml"; // The file you created in resources
        int exitCode = cmd.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps", resourcesDir
                        .resolve("all-reference-types-apps")
                        .resolve("app-resource")
                        .resolve("more-resources")
                        .resolve("extra-resource.json").toString() // The "modified" file
        );
        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testBulkQuery() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "bulk-apps.yaml"; // The file you created in resources
        Path complexApps = resourcesDir.resolve("complex-apps");
        Path appConfigMap = resourcesDir.resolve("all-reference-types-apps").resolve("app-configmap");
        Path circularDependencyAppsPath = resourcesDir.resolve("circular-dependency-apps");
        String someDeploymentJson = complexApps.resolve("subset-with-circular").resolve("some-deployment.JSON").toString();
        String configMapEnv = appConfigMap.resolve("configmap1.env").toString();
        String app2ResourceYaml = circularDependencyAppsPath.resolve("app2").resolve("app2-resource.yaml").toString();

        int exitCode = cmd.execute(
                "--apps-dir", complexApps.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                someDeploymentJson,
                configMapEnv,
                app2ResourceYaml
        );

        assertTrue(getCapturedOut().contains("Circular dependency detected"),
                "Error messages missmatch\n" +
                        "Circular dependency detected \n" +
                        "Actual: " + getCapturedOut());
        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }


    @Test
    void testBulkFileQuery() throws IOException {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "bulk-file-apps.yaml"; // The file you created in resources
        Path listFile = tempDir.resolve("my-list-of-modified-files.txt");

        String baseResourceOneYaml = resourcesDir
                .resolve("all-reference-types-apps")
                .resolve("app-base")
                .resolve("base-directory-reference")
                .resolve("base-resource-one.yaml").toString();
        String baseResourceTwoYaml = resourcesDir
                .resolve("app-with-unparseable-kustomization")
                .resolve("valid-kustomization-base")
                .resolve("base-resource-two.yml").toString();

        List<String> pathsToPutInListFile = Arrays.asList(
                baseResourceOneYaml,
                baseResourceTwoYaml,
                "non-existing.yaml"
        );
        Files.write(listFile, pathsToPutInListFile, StandardCharsets.UTF_8);

        int exitCode = cmd.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                "--files-from-file", listFile.toString()
        );

        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testErrorRedirection_NonExistentFile() throws IOException {
        Path appsActualDir = Files.createDirectory(tempDir.resolve("apps"));
        Path appsActualFile = Files.createDirectory(tempDir.resolve("apps").resolve("some-file.yaml"));
        Path nonExistingFile = tempDir.resolve("non-existing.file");

        int exitCode = cmd.execute(
                "--apps-dir", appsActualDir.toString(),
                "affected-apps",
                "--files-from-file", nonExistingFile.toString(),
                appsActualFile.toString()
        );

        assertEquals(1, exitCode, "Exit code should be 1 (custom command error for invalid app path)");

        String expectedFullErrorMessage = "Error: File specified by --files-from-file not found: " + nonExistingFile.toAbsolutePath();

        assertEquals(getCapturedErr(), expectedFullErrorMessage, "Log file should contain the exact invalid app path error. " +
                "\nExpected to find: '" + expectedFullErrorMessage + "'" +
                "\nActual:           '" + getCapturedErr() + "'");
    }

    @Test
    void testErrorRedirection_EmptyFile() throws IOException {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "empty-bulk-file-apps.yaml"; // The file you created in resources
        Path listFile = tempDir.resolve("my-empty-list.txt");
        Files.write(listFile, Collections.emptyList(), StandardCharsets.UTF_8);

        int exitCode = cmd.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                "--files-from-file", listFile.toString()
        );

        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }
}
