package lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import graph.KustomGraphBuilder;
import model.KustomGraph;
import model.Kustomization;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import parser.ReferenceType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static parser.ReferenceType.*;

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
                    assertThat(event.getMessage()).contains(type.toString());
                    assertThat(event.getMessage()).contains(cause);
                    assertThat(event.getMessage()).contains(Path.of(file).getFileName().toString());
                });
    }

    @BeforeAll
    static void buildGraphForAllReferenceTypes() throws IOException {
        appsDir = Paths.get( "src", "test", "resources", "all-reference-types-apps");
        KustomGraphBuilder builder = new KustomGraphBuilder(appsDir);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        graph = builder.build();

        REFERENCE_FOLDERS
                .forEach(referenceFolder -> {
                    Path appPath = appsDir.resolve(referenceFolder).resolve("kustomization.yaml");
                    Kustomization app = (Kustomization) graph.getNode(appPath);
                    assertNotNull(app, referenceFolder + " kustomization should exist");
                    apps.put(referenceFolder, app);
                });
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
        assertInvalidReference("invalid", "Non string value found", app, COMPONENT, Level.WARN);
    }

    @Test
    void testInvalidPatchReferences() {
        Kustomization app = apps.get("app-patch");

        assertInvalidReference("non-existing-patch.yaml", "Non-Existing file referenced", app, PATCH, Level.ERROR);
        assertInvalidReference("invalid-directory", "Not a valid kubernetes resource", app, PATCH, Level.WARN);
        assertInvalidReference("invalid", "Non string value found", app, PATCH, Level.WARN);
    }

    @Test
    void testInvalidPatchMergeReferences() {
        Kustomization app = apps.get("app-patch-merge");
        String cause = "Not a valid kubernetes resource";

        assertInvalidReference(Path.of("invalid-directory").resolve("kustomization.yaml").toString(), cause, app, PATCH_MERGE, Level.WARN);
        assertInvalidReference("invalid-directory", cause, app, PATCH_MERGE, Level.WARN);
    }

    @Test
    void testInvalidResourceReferences() {
        Kustomization app = apps.get("app-resource");

        assertInvalidReference("non-existing-resource", "Non-Existing file referenced", app, RESOURCE, Level.ERROR);
        assertInvalidReference(Path.of("invalid-directory").resolve("kustomization.yaml").toString(), "Not a valid kubernetes resource", app, RESOURCE, Level.WARN);
        assertInvalidReference("invalid", "Multiline value found", app, RESOURCE, Level.WARN);
    }
}
