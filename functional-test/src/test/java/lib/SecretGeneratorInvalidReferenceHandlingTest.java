package lib;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import dev.zucca_ops.kustomtrace.model.Kustomization;
import dev.zucca_ops.kustomtrace.parser.ReferenceExtractors;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class SecretGeneratorInvalidReferenceHandlingTest {

    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    Kustomization app;

    private void assertInvalidReference(String file) {
        assertThat(app.getDependencies())
                .noneMatch(path -> path.toString().contains(file));

        assertThat(listAppender.list)
                .anySatisfy(event -> {
                    assertThat(event.getLevel()).isEqualTo(Level.WARN);
                    String formattedEvent = event.getFormattedMessage();
                    assertThat(formattedEvent).contains("Invalid reference");
                    assertThat(formattedEvent).contains(file);
                });
    }

    @Test
    void testInvalidSecretGeneratorReferences() throws IOException {
        Logger logger = (Logger) LoggerFactory.getLogger(ReferenceExtractors.class);
        Path appsDir = Paths.get("src", "test", "resources", "all-reference-types-apps", "app-secret");
        listAppender.start();
        logger.addAppender(listAppender);
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDir);
        KustomGraph graph = kustomTrace.getGraph();

        Path appPath = appsDir.resolve("kustomization.yaml");
        app = graph.getKustomization(appPath);

        assertInvalidReference("non-existing-secret.txt");
        assertInvalidReference("invalid-directory");

        assertThat(app.getDependencies())
                .noneMatch(path -> path.toString().contains("ignored.yaml"));

        logger.detachAppender(listAppender);
    }
}
