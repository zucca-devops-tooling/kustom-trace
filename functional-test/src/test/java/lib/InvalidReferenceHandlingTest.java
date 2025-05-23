package lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import dev.zucca_ops.kustomtrace.parser.ReferenceType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static dev.zucca_ops.kustomtrace.parser.ReferenceType.*;

public class InvalidReferenceHandlingTest {

    private static KustomGraph graph;
    private static Path appsDir;

    private static final Map<String, Kustomization> apps = new HashMap<>();

    private static final Logger logger = (Logger) LoggerFactory.getLogger(ReferenceType.class);
    private static ListAppender<ILoggingEvent> listAppender;


    private static final List<String> REFERENCE_FOLDERS = List.of(
            "app-base",
            "app-component",
            "app-configmap",
            "app-patch",
            "app-patch-merge",
            "app-resource"
    );

    private void assertInvalidReference(String file, String cause, Kustomization app, ReferenceType type, Level level) {
        // Assert that no dependencies of app contain the invalid parts in their paths
        assertThat(app.getDependencies())
                .noneMatch(path -> path.toString().contains(file));

        // Assert that invalid references were logged
        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(level);
                    String formattedEvent = event.getFormattedMessage();
                    assertThat(formattedEvent).contains(type.toString());
                    assertThat(formattedEvent).contains(cause);
                    assertThat(formattedEvent).contains(Path.of(file).getFileName().toString());
                });
    }

    @BeforeAll
    static void buildGraphForAllReferenceTypes() throws IOException {
        appsDir = Paths.get( "src", "test", "resources", "all-reference-types-apps");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
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

    @AfterAll
    static void tearDown() {
        // Clean up the appender
        logger.detachAppender(listAppender);
    }


    @Test
    void testInvalidBaseReferences() {
        Kustomization app = apps.get("app-base");

        String cause = "Invalid directory or kustomization file";

        assertInvalidReference("non-existing-base", cause, app, BASE, Level.WARN);
        assertInvalidReference("invalid-reference.yaml", cause, app, BASE, Level.WARN);
    }

    @Test
    void testInvalidComponentReferences() {
        Kustomization app = apps.get("app-component");

        assertInvalidReference("non-existing-component", "Invalid directory or kustomization file", app, COMPONENT, Level.WARN);
        assertInvalidReference("invalid", "Non-string value found for reference", app, COMPONENT, Level.WARN);
    }

    @Test
    void testInvalidPatchReferences() {
        Kustomization app = apps.get("app-patch");

        assertInvalidReference("non-existing-patch.yaml", "Non-existing or non-regular file", app, PATCH, Level.ERROR);
        assertInvalidReference("invalid-directory", "Not a valid Kubernetes resource", app, PATCH, Level.WARN);
    }

    @Test
    void testInvalidPatchMergeReferences() {
        Kustomization app = apps.get("app-patch-merge");
        String cause = "Not a valid Kubernetes resource";

        assertInvalidReference(Path.of("invalid-directory").resolve("kustomization.yaml").toString(), cause, app, PATCH_MERGE, Level.WARN);
        assertInvalidReference("invalid-directory", cause, app, PATCH_MERGE, Level.WARN);
    }

    @Test
    void testInvalidResourceReferences() {
        Kustomization app = apps.get("app-resource");

        assertInvalidReference("non-existing-resource", "Non-existing or non-regular file", app, RESOURCE, Level.ERROR);
        assertInvalidReference(Path.of("invalid-directory").resolve("kustomization.yaml").toString(), "Not a valid Kubernetes resource", app, RESOURCE, Level.WARN);
        assertInvalidReference("invalid", "Multiline value found", app, RESOURCE, Level.WARN);
    }
}
