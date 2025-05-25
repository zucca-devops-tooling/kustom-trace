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

public class ListRootAppsTest {

    private final Path resourcesDir = Paths.get( "src", "test", "resources");
    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("root-apps");
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
    void testOverallResources() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "overall.yaml";

        int exitCode = cmd.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "list-root-apps"
        );
        assertEquals(0, exitCode);

        String expectedError = "ResourceReferenceResolver - Error parsing content for dependency type 'BASE'";
        assertTrue(getCapturedOut().contains(expectedError),
                "Make sure at least one of the expected errors like " + expectedError + " is logged \n" +
                        "Actual: " + getCapturedOut());

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testEmptyBecauseCircularApp() {
        Path actualOutputFile = tempDir.resolve("affected-apps-actual-output.yaml");
        String expectedResourceFileName = "empty.yaml";
        Path appsPath = resourcesDir.resolve("circular-dependency-apps").resolve("app2");

        int exitCode = cmd.execute(
                "--apps-dir", appsPath.toString(),
                "--output", actualOutputFile.toString(),
                "list-root-apps"
        );
        assertEquals(0, exitCode);

        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }
}

