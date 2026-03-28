package nativecli;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;

abstract class NativeCliSmokeTestSupport {

    protected final Path resourcesDir = Paths.get("src", "test", "resources").toAbsolutePath().normalize();
    protected NativeCliExecutor nativeCliExecutor;

    @BeforeEach
    void setUpNativeCli() {
        nativeCliExecutor = new NativeCliExecutor(System.getProperty("nativeCliPath"));
        nativeCliExecutor.assertExecutableExists();
    }
}
