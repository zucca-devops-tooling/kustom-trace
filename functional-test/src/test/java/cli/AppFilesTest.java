package cli;

import cli.util.OutputResourceAssesor;
import cli.util.WriterOutputStream;
import dev.zucca_ops.kustomtrace.cli.KustomTraceCLI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppFilesTest {

    private final Path resourcesDir = Paths.get( "src", "test", "resources");
    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("app-files");
    private CommandLine cmd;
    private StringWriter swOut;

    private PrintStream originalOut;


    private String getCapturedOut() { return swOut.toString().trim(); }

    @TempDir
    Path tempDir;
    @BeforeEach
    void setUp() {
        originalOut = System.out;

        swOut = new StringWriter();
        System.setOut(new PrintStream(new WriterOutputStream(swOut, StandardCharsets.UTF_8.name()), true));

        cmd = new CommandLine(new KustomTraceCLI());
        System.clearProperty("log.file");
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.clearProperty("log.file");
    }

    @Test
    void testConfigmapApp() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "configmap-files.yaml";
        Path appsPath = resourcesDir.resolve("all-reference-types-apps");

        int exitCode = cmd.execute(
                "--apps-dir", appsPath.toString(),
                "app-files", appsPath.resolve("app-configmap").toString(),
                "--output", actualOutputFile.toString()
        );
        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testUnparseableApp() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "app-with-unparseables.yaml";
        Path appsPath = resourcesDir.resolve("app-with-unparseable-kustomization");

        int exitCode = cmd.execute(
                "--apps-dir", appsPath.toString(),
                "app-files", appsPath.toString(),
                "--output", actualOutputFile.toString()
        );
        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testSubsetWithCircularApp() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "subset-with-circular.yaml";
        Path appPath = resourcesDir.resolve("complex-apps").resolve("subset-with-circular");

        int exitCode = cmd.execute(
                "--apps-dir", resourcesDir.toString(),
                "app-files", appPath.toString(),
                "--output", actualOutputFile.toString()
        );
        assertEquals(0, exitCode);
        assertTrue(getCapturedOut().contains("Circular dependency detected"),
                "Error messages missmatch\n" +
                        "Circular dependency detected \n" +
                        "Actual: " + getCapturedOut());

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testNotRootApp() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "not-root-app.yaml";
        Path appsPath = resourcesDir.resolve("complex-apps").resolve("root-and-all-references");
        Path appPath = resourcesDir.resolve("all-reference-types-apps").resolve("app-base");

        int exitCode = cmd.execute(
                "--apps-dir", appsPath.toString(),
                "app-files", appPath.toString(),
                "--output", actualOutputFile.toString()
        );
        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }
}
