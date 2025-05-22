package lib;

import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.model.KustomGraph;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class NoKustomizationAppsTest {
    @Test
    void testEmptyGraphIsReturned() throws IOException {
        Path appsDir = Paths.get("src", "test", "resources", "app-with-no-kustomization");

        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDir);
        KustomGraph graph = kustomTrace.getGraph();

        assertThat(graph.getRootApps().size()).isEqualTo(0);
    }
}
