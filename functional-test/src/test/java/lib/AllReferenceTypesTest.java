package lib;

import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.model.KustomFile;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AllReferenceTypesTest {
    private static KustomGraph graph;
    private static Path appsDir;

    private static final Map<String, Kustomization> apps = new HashMap<>();

    private static final List<String> REFERENCE_FOLDERS = List.of(
            "app-base",
            "app-component",
            "app-configmap",
            "app-patch",
            "app-patch-merge",
            "app-resource"
    );

    @BeforeAll
    static void buildGraphForAllReferenceTypes() throws IOException {
        appsDir = Paths.get( "src", "test", "resources", "all-reference-types-apps");
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDir);
        graph = kustomTrace.getGraph();

        REFERENCE_FOLDERS
                .forEach(referenceFolder -> {
                    Path appPath = appsDir.resolve(referenceFolder).resolve("kustomization.yaml");
                    Kustomization app = (Kustomization) graph.getNode(appPath);
                    assertNotNull(app, referenceFolder + " kustomization should exist");
                    apps.put(referenceFolder, app);
                });
    }

    private Path assertDependency(String folder, String filename, String appName) {
        Kustomization app = apps.get(appName);
        Optional<Path> dependency = app.getDependencies()
                .filter(path -> path.getParent().endsWith(folder))
                .filter(path -> path.endsWith(filename))
                .findFirst();

        assertTrue(dependency.isPresent());

        return dependency.get();
    }

    private void assertDirectoryDependency(String appName, String folderName, String kustomizationName, String resource, String kind) {
        Path dependency = assertDependency(folderName, resource, appName);
        KustomFile dependencyNode = graph.getKustomFile(dependency);
        assertNotNull(dependencyNode);

        assertThat(dependencyNode.getResource().getKind()).isEqualTo(kind);

        dependency = assertDependency(folderName, kustomizationName, appName);
        Kustomization kustomization = graph.getKustomization(dependency);
        assertNotNull(kustomization);
    }

    private void assertFileDependency(String appName, String resource, String kind) {
        assertFileDependency(appName, appName, resource, kind);
    }

    private void assertFileDependency(String appName, String folder, String resource, String kind) {
        Path dependency = assertDependency(folder, resource, appName);
        KustomFile dependencyNode = graph.getKustomFile(dependency);
        assertNotNull(dependencyNode);

        assertThat(dependencyNode.getResource().getKind()).isEqualTo(kind);
    }

    @Test
    void testBaseKustomizationReference() {
        String folderName = "base-kustomization-reference";
        String appName = "app-base";
        String resource = "base-resource-one.yaml";
        String kind = "ConfigMap";
        String kustomizationName = "kustomization.yaml";

        assertDirectoryDependency(appName, folderName, kustomizationName, resource, kind);
    }

    @Test
    void testBaseDirectoryReference() {
        String folderName = "base-directory-reference";
        String appName = "app-base";
        String resource = "base-resource-two.yml";
        String kind = "Secret";
        String kustomizationName = "kustomization.yml";

        assertDirectoryDependency(appName, folderName, kustomizationName, resource, kind);
    }

    @Test
    void testComponentDirectoryReference1() {
        String folderName = "component-directory-reference";
        String appName = "app-component";
        String resource = "component-one.yaml";
        String kind = "Service";
        String kustomizationName = "kustomization.yaml";

        assertDirectoryDependency(appName, folderName, kustomizationName, resource, kind);
    }

    @Test
    void testComponentDirectoryReference2() {
        String folderName = "component-directory-reference";
        String appName = "app-component";
        String resource = "component-two.json";
        String kind = "Deployment";
        String kustomizationName = "kustomization.yaml";

        assertDirectoryDependency(appName, folderName, kustomizationName, resource, kind);
    }

    @Test
    void testConfigmapEnv() {
        String appName = "app-configmap";
        String resource = "configmap1.env";
        String kind = "Undefined";
        assertFileDependency(appName, resource, kind);
    }

    @Test
    void testConfigmapAssignedEnv() {
        String appName = "app-configmap";
        String resource = "configmap2.env";
        String kind = "Undefined";
        assertFileDependency(appName, resource, kind);
    }

    @Test
    void testConfigmapTxtFile() {
        String appName = "app-configmap";
        String resource = "plain-text-file.txt";
        String kind = "Undefined";
        assertFileDependency(appName, resource, kind);
    }

    @Test
    void testPatchReference() {
        String appName = "app-patch";
        String resource = "patch1.yaml";
        String kind = "Deployment";
        assertFileDependency(appName, resource, kind);
    }

    @Test
    void testInlinePatchReference() {
        String appName = "app-patch";
        String resource = "patch2.yaml";
        String kind = "Deployment";
        assertFileDependency(appName, resource, kind);
    }

    @Test
    void testInlinePatchMergeReference1() {
        String appName = "app-patch-merge";
        String resource = "patch-merge1.yaml";
        String kind = "Service";
        assertFileDependency(appName, resource, kind);
    }

    @Test
    void testInlinePatchMergeReference2() {
        String appName = "app-patch-merge";
        String resource = "patch-merge2.yaml";
        String kind = "Service";
        assertFileDependency(appName, resource, kind);
    }

    @Test
    void testResourceFileReference1() {
        String appName = "app-resource";
        String folderName = "more-resources";
        String resource = "extra-resource.yml";
        String kind = "Service";
        assertFileDependency(appName, folderName, resource, kind);
    }

    @Test
    void testResourceFileReference2() {
        String appName = "app-resource";
        String folderName = "more-resources";
        String resource = "extra-resource.json";
        String kind = "Pod";
        assertFileDependency(appName, folderName, resource, kind);
    }

    @Test
    void testResourceSelfDirectoryReference1() {
        String folderName = "app-resource";
        String appName = "app-resource";
        String resource = "my-resource.yaml";
        String kind = "Service";
        String kustomizationName = "kustomization.yaml";

        assertDirectoryDependency(appName, folderName, kustomizationName, resource, kind);
    }

    @Test
    void testResourceSelfDirectoryReference2() {
        String folderName = "app-resource";
        String appName = "app-resource";
        String resource = "another-resource.yaml";
        String kind = "Deployment";
        String kustomizationName = "kustomization.yaml";

        assertDirectoryDependency(appName, folderName, kustomizationName, resource, kind);
    }

    @Test
    void testAppAmount() {
        // We expect 6 root apps in total (1 per type) and 3 isolated/bad referenced kustomization.yaml
        assertThat(graph.getRootApps().size()).isEqualTo(9);
    }
}
