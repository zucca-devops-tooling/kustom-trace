package lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.graph.KustomGraphBuilder;
import dev.zucca_ops.kustomtrace.graph.ResourceReferenceResolver;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class InvalidFileHandlingTest {
    private static Path appsDir;

    private static final Logger logger = (Logger) LoggerFactory.getLogger(ResourceReferenceResolver.class);
    private static ListAppender<ILoggingEvent> listAppender;

    private static Kustomization app;

    @BeforeAll
    static void buildGraphForAllReferenceTypes() throws IOException {
        appsDir = Paths.get( "src", "test", "resources", "app-with-unparseable-kustomization");
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDir);
        KustomGraph graph = kustomTrace.getGraph();

        Path appPath = appsDir.resolve("kustomization.yaml");
        app = graph.getKustomization(appPath);
        assertNotNull(app,  "kustomization should exist");
    }

    @AfterAll
    static void tearDown() {
        // Clean up the appender
        logger.detachAppender(listAppender);
    }

    @Test
    void testValidSubAppIsCorrectlyBuilt() {
        Optional<Path> dependency = app.getDependencies()
                .filter(path -> path.getParent().endsWith("valid-kustomization-base"))
                .filter(path -> path.endsWith("base-resource-two.yml"))
                .findFirst();

        assertTrue(dependency.isPresent());
    }


    @Test
    void testInvalidSubAppIsNotBuilt() {
        // Assert that no dependencies of app contain the invalid parts in their paths
        assertThat(app.getDependencies())
                .noneMatch(path -> path.getParent().endsWith("invalid-kustomization-base"))
                .noneMatch(path -> path.endsWith("invalid-content-deployment.json"));

        // Assert that invalid files were logged
        Level level = Level.ERROR;
        String cause = "invalid yaml/json content";
        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(level);
                    assertThat(event.getMessage()).contains(appsDir.resolve("invalid-kustomization-base/kustomization.yaml").toString());
                    assertThat(event.getMessage()).contains(cause);
                });


        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(level);
                    assertThat(event.getMessage()).contains("invalid-content-deployment.json");
                    assertThat(event.getMessage()).contains(cause);
                });
    }
}