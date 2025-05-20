package lib;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import graph.KustomGraphBuilder;
import model.KustomGraph;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class NoKustomizationAppsTest {
    @Test
    void testEmptyGraphIsReturned() throws IOException {
        Path appsDir = Paths.get("src", "test", "resources", "app-with-no-kustomization");
        KustomGraphBuilder builder = new KustomGraphBuilder(appsDir);
        KustomGraph graph = builder.build();

        assertThat(graph.getApps().size()).isEqualTo(0);
    }
}
