package lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import graph.KustomGraphBuilder;
import model.KustomGraph;
import model.Kustomization;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import parser.ReferenceExtractors;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMapInvalidReferenceHandlingTest {

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    Kustomization app;

    private void assertInvalidReference(String file, String cause, Level level) {
        // Assert that no dependencies of app contain the invalid parts in their paths
        assertThat(app.getDependencies())
                .noneMatch(path -> path.toString().contains(file));

        // Assert that invalid references were logged
        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(level);
                    assertThat(event.getMessage()).contains(cause);
                    assertThat(event.getMessage()).contains(file);
                });
    }


    @Test
    void testInvalidConfigmapReferences() throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(ReferenceExtractors.class);
        Path appsDir = Paths.get( "src", "test", "resources", "all-reference-types-apps", "app-configmap");
        KustomGraphBuilder builder = new KustomGraphBuilder(appsDir);
        listAppender.start();
        logger.addAppender(listAppender);
        KustomGraph graph = builder.build();

        Path appPath = appsDir.resolve("kustomization.yaml");
        app = (Kustomization) graph.getNode(appPath);

        assertInvalidReference("non-existing-configmap.yaml", "Invalid reference", Level.WARN);
        assertInvalidReference("invalid-directory", "Invalid reference", Level.WARN);

        assertThat(app.getDependencies())
                .noneMatch(path -> path.toString().contains("ignored.yaml"));
    }
}
