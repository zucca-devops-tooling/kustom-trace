package lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import dev.zucca_ops.kustomtrace.parser.ReferenceExtractors;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigMapInvalidReferenceHandlingTest {

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    Kustomization app;

    private void assertInvalidReference(String file) {
        // Assert that no dependencies of app contain the invalid parts in their paths
        assertThat(app.getDependencies())
                .noneMatch(path -> path.toString().contains(file));

        // Assert that invalid references were logged
        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    String formattedEvent = event.getFormattedMessage();
                    assertThat(formattedEvent).contains("Invalid reference");
                    assertThat(formattedEvent).contains(file);
                });
    }


    @Test
    void testInvalidConfigmapReferences() throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(ReferenceExtractors.class);
        Path appsDir = Paths.get( "src", "test", "resources", "all-reference-types-apps", "app-configmap");
        listAppender.start();
        logger.addAppender(listAppender);
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDir);
        KustomGraph graph = kustomTrace.getGraph();

        Path appPath = appsDir.resolve("kustomization.yaml");
        app = graph.getKustomization(appPath);

        assertInvalidReference("non-existing-configmap.yaml");
        assertInvalidReference("invalid-directory");

        assertThat(app.getDependencies())
                .noneMatch(path -> path.toString().contains("ignored.yaml"));

        // Clean up the appender
        logger.detachAppender(listAppender);
    }
}
