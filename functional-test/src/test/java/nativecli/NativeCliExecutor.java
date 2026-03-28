package nativecli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class NativeCliExecutor {

    private final Path executable;

    public NativeCliExecutor(String executablePath) {
        this.executable = Path.of(Objects.requireNonNull(executablePath, "nativeCliPath must be set"))
                .toAbsolutePath()
                .normalize();
    }

    public Path getExecutable() {
        return executable;
    }

    public void assertExecutableExists() {
        if (!Files.isRegularFile(executable)) {
            throw new IllegalStateException("Native CLI binary not found: " + executable);
        }
    }

    public NativeCliResult execute(String... arguments) {
        List<String> command = new ArrayList<>();
        command.add(executable.toString());
        command.addAll(List.of(arguments));

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        try {
            Process process = processBuilder.start();
            CompletableFuture<String> stdout = readAsync(process.getInputStream());
            CompletableFuture<String> stderr = readAsync(process.getErrorStream());
            int exitCode = process.waitFor();
            return new NativeCliResult(exitCode, stdout.get().trim(), stderr.get().trim());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start native CLI: " + executable, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for native CLI to finish", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Failed to capture native CLI output", e);
        }
    }

    private static CompletableFuture<String> readAsync(InputStream stream) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream inputStream = stream) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read process output", e);
            }
        });
    }
}
