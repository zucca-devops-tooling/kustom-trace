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
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CircularDependencyTest {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Kustomization.class);

    private Path getTestResourcePath() {
        return Paths.get("src", "test", "resources", "circular-dependency-apps", "");
    }

    @Test
    void testDuplicateProcessingWithResources() throws IOException {
        // Setup Logback appender to capture log events
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        Path appsDir = getTestResourcePath();
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDir);
        KustomGraph graph = kustomTrace.getGraph();
        Path app1Path = appsDir.resolve("app1").resolve("kustomization.yaml");
        Path app2Path = appsDir.resolve("app2").resolve("kustomization.yaml");
        Path app1ComponentPath = appsDir.resolve("app1").resolve("component").resolve("kustomization.yaml");
        Path app2ComponentPath = appsDir.resolve("app2").resolve("component").resolve("kustomization.yaml");
        Path app1ResourcePath = appsDir.resolve("app1").resolve("app1-resource.yaml");
        Path app2ResourcePath = appsDir.resolve("app2").resolve("app2-resource.yaml");
        Path component1ResourcePath = appsDir.resolve("app1").resolve("component").resolve("component1-resource.yaml");
        Path component2ResourcePath = appsDir.resolve("app2").resolve("component").resolve("component2-resource.yaml");

        Kustomization app1 = (Kustomization) graph.getNode(app1Path);
        Kustomization app2 = (Kustomization) graph.getNode(app2Path);

        assertNotNull(app1);
        assertNotNull(app2);

        // Call the methods that should trigger the warning
        List<Path> app1Dependencies = app1.getDependencies().toList();
        List<Kustomization> app1Apps = app1.getApps().toList();

        // Assert that a warning about circular dependency was logged
        List<ILoggingEvent> warnings = listAppender.list.stream()
                .filter(event -> event.getLevel() == Level.ERROR && event.getMessage().contains("Circular dependency detected"))
                .toList();
        assertEquals(2, warnings.size(), "Should have logged two circular dependency warnings.");
        assertThat(warnings).allMatch(warning -> warning.getMessage().contains("Circular"));

        // Assert getDependencies() functions correctly (doesn't loop infinitely)
        assertThat(app1Dependencies).contains(app1Path);
        assertThat(app1Dependencies).contains(app1ResourcePath);
        assertThat(app1Dependencies).contains(app1ComponentPath);
        assertThat(app1Dependencies).contains(app2Path);
        assertThat(app1Dependencies).contains(app2ResourcePath);
        assertThat(app1Dependencies).contains(app2ComponentPath);
        assertThat(app1Dependencies).contains(component1ResourcePath);
        assertThat(app1Dependencies).contains(component2ResourcePath);
        assertEquals(8, app1Dependencies.size());

        // There shoudl be no apps, as they are circularly dependent
        assertEquals(0, app1Apps.size());

        // Clean up the appender
        logger.detachAppender(listAppender);
    }
}
