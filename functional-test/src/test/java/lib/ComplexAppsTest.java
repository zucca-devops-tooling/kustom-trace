package lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ComplexAppsTest {

    private static KustomGraph graph;
    private static Path appsDir;


    @BeforeAll
    static void buildGraphForAllReferenceTypes() throws IOException {
        appsDir = Paths.get( "src", "test", "resources", "complex-apps");
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDir);
        graph = kustomTrace.getGraph();
        assertThat(graph.getRootApps().size()).isEqualTo(2); // subset-with-circular and root-and-all-references
    }

    private Path appResolve(Path path) {
        return path.resolve("kustomization.yaml").normalize();
    }

    @Test
    void testAllReferences() {
        List<String> referenceFolders = List.of(
                "app-base",
                "app-component",
                "app-configmap",
                "app-patch",
                "app-patch-merge",
                "app-resource"
        );

        Kustomization app = graph.getKustomization(appResolve(appsDir.resolve("all-references")));
        assertNotNull(app);
        Path referencesAppFolder = appsDir.resolve("../").resolve("all-reference-types-apps");

        List<Path> dependencies = app.getDependencies().toList();

        referenceFolders.forEach(folder -> assertThat(dependencies).contains(appResolve(referencesAppFolder.resolve(folder))));
    }

    @Test
    void testAllInvalidApps() {
        Kustomization app = graph.getKustomization(appResolve(appsDir.resolve("invalid-apps")));
        assertNotNull(app);
        Path resourcesFolder = appsDir.resolve("../");

        List<Path> dependencies = app.getDependencies().toList();

        assertThat(dependencies).contains(appResolve(resourcesFolder.resolve("app-with-unparseable-kustomization")));
        assertThat(dependencies).noneMatch(path -> path.toString().contains("app-with-no-kustomization")); // no kustomization available there
        assertThat(dependencies.size()).isEqualTo(4); // itself, the two kustomizations at unparseable folder and the yml resource of it
    }

    @Test
    void testRootAndAllReferences() {
        Kustomization app = graph.getKustomization(appResolve(appsDir.resolve("root-and-all-references")));
        assertNotNull(app);

        List<Path> dependencies = app.getDependencies().toList();

        assertThat(dependencies).contains(appResolve(appsDir)); // root
        assertThat(dependencies).contains(appResolve(appsDir.resolve("invalid-apps"))); // as root reference
        assertThat(dependencies).contains(appResolve(appsDir.resolve("all-references")));
        assertThat(dependencies).noneMatch(path -> path.toString().contains("app-with-no-kustomization")); // still should not pick them up
    }

    @Test
    void testSubsetWithCircular() {
        List<String> subsetFolders = List.of(
                "app-base",
                "app-configmap",
                "app-resource"
        );
        Path referencesAppFolder = appsDir.resolve("../").resolve("all-reference-types-apps");
        Path appPath = appsDir.resolve("subset-with-circular");
        Kustomization app = graph.getKustomization(appResolve(appPath));
        assertNotNull(app);

        Logger logger = (Logger) LoggerFactory.getLogger(Kustomization.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        List<Path> dependencies = app.getDependencies().toList();

        // Assert that a warning about circular dependency was logged
        List<ILoggingEvent> warnings = listAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR && event.getMessage().contains("Circular dependency detected"))
                .toList();
        assertEquals(1, warnings.size(), "Should have logged a circular dependency warning.");
        assertThat(warnings).allMatch(warning -> warning.getMessage().contains("Circular"));

        assertThat(dependencies).contains(appPath.resolve("some-deployment.JSON"));
        subsetFolders.forEach(folder -> assertThat(dependencies).contains(appResolve(referencesAppFolder.resolve(folder))));
        subsetFolders.forEach(folder -> assertThat(graph.getKustomization(appResolve(referencesAppFolder.resolve(folder))).getApps().count()).isEqualTo(2)); //all-references and this one

        logger.detachAppender(listAppender);
    }
}
