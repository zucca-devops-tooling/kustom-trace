package dev.zucca_ops.kustomtrace.model;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphNodeConcurrencyTest {

    @Test
    void addDependentRetainsAllUniqueDependentsWhenCalledConcurrently() throws Exception {
        KustomFile sharedFile = new KustomFile(Path.of("base", "common-base.yaml"));
        Kustomization app1 = new Kustomization(Path.of("apps", "app1", "kustomization.yaml"), Map.of());
        Kustomization app2 = new Kustomization(Path.of("apps", "app2", "kustomization.yaml"), Map.of());

        runConcurrently(
                () -> sharedFile.addDependent(app1),
                () -> sharedFile.addDependent(app2),
                500);

        assertEquals(2, sharedFile.getDependents().size());
        assertTrue(sharedFile.getDependents().containsAll(List.of(app1, app2)));
    }

    private void runConcurrently(Runnable first, Runnable second, int repetitions) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<?>[] futures = new Future<?>[repetitions * 2];

            for (int i = 0; i < repetitions; i++) {
                futures[i * 2] = executor.submit(() -> awaitAndRun(start, first));
                futures[i * 2 + 1] = executor.submit(() -> awaitAndRun(start, second));
            }

            start.countDown();

            for (Future<?> future : futures) {
                future.get();
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }
    }

    private void awaitAndRun(CountDownLatch start, Runnable operation) {
        try {
            start.await();
            operation.run();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to start concurrent operation", e);
        }
    }
}
