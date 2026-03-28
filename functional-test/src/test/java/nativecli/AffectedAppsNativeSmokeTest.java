package nativecli;

import cli.util.OutputResourceAssesor;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AffectedAppsNativeSmokeTest extends NativeCliSmokeTestSupport {

    private final OutputResourceAssesor outputResourceAssesor = new OutputResourceAssesor("affected-apps");

    @TempDir
    Path tempDir;

    @Test
    void testCircularDependency() {
        Path actualOutputFile = tempDir.resolve("affected-apps-native-output.yaml");
        String expectedResourceFileName = "circular-dependency.yaml";
        Path circularDependencyAppsPath = resourcesDir.resolve("circular-dependency-apps");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", circularDependencyAppsPath.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps", circularDependencyAppsPath.resolve("app1").resolve("app1-resource.yaml").toString()
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testAllApps() {
        Path actualOutputFile = tempDir.resolve("affected-apps-native-output.yaml");
        String expectedResourceFileName = "all-apps.yaml";

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                resourcesDir
                        .resolve("all-reference-types-apps")
                        .resolve("app-resource")
                        .resolve("more-resources")
                        .resolve("extra-resource.json")
                        .toString()
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testBulkQuery() {
        Path actualOutputFile = tempDir.resolve("affected-apps-native-output.yaml");
        String expectedResourceFileName = "bulk-apps.yaml";
        Path complexApps = resourcesDir.resolve("complex-apps");
        Path appConfigMap = resourcesDir.resolve("all-reference-types-apps").resolve("app-configmap");
        Path circularDependencyAppsPath = resourcesDir.resolve("circular-dependency-apps");
        String someDeploymentJson = complexApps.resolve("subset-with-circular").resolve("some-deployment.JSON").toString();
        String configMapEnv = appConfigMap.resolve("configmap1.env").toString();
        String app2ResourceYaml = circularDependencyAppsPath.resolve("app2").resolve("app2-resource.yaml").toString();

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", complexApps.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                someDeploymentJson,
                configMapEnv,
                app2ResourceYaml
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testBulkFileQuery() throws IOException {
        Path actualOutputFile = tempDir.resolve("affected-apps-native-output.yaml");
        String expectedResourceFileName = "bulk-file-apps.yaml";
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

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                "--files-from-file", listFile.toString()
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }

    @Test
    void testErrorRedirectionNonExistentFile() throws IOException {
        Path appsActualDir = Files.createDirectory(tempDir.resolve("apps"));
        Path appsActualFile = Files.createDirectory(tempDir.resolve("apps").resolve("some-file.yaml"));
        Path nonExistingFile = tempDir.resolve("non-existing.file");

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", appsActualDir.toString(),
                "affected-apps",
                "--files-from-file", nonExistingFile.toString(),
                appsActualFile.toString()
        );

        assertEquals(1, result.exitCode());

        String expectedFullErrorMessage =
                "Error: File specified by --files-from-file not found: " + nonExistingFile.toAbsolutePath();

        assertEquals(expectedFullErrorMessage, result.stderr());
    }

    @Test
    void testErrorRedirectionEmptyFile() throws IOException {
        Path actualOutputFile = tempDir.resolve("affected-apps-native-output.yaml");
        String expectedResourceFileName = "empty-bulk-file-apps.yaml";
        Path listFile = tempDir.resolve("my-empty-list.txt");
        Files.write(listFile, Collections.emptyList(), StandardCharsets.UTF_8);

        NativeCliResult result = nativeCliExecutor.execute(
                "--apps-dir", resourcesDir.toString(),
                "--output", actualOutputFile.toString(),
                "affected-apps",
                "--files-from-file", listFile.toString()
        );

        assertEquals(0, result.exitCode());
        outputResourceAssesor.assertYamlOutputMatchesResource(actualOutputFile, expectedResourceFileName);
    }
}
