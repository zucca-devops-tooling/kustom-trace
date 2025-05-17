import picocli.CommandLine;
import picocli.CommandLine.IFactory;

import java.io.File;

public class KustomTraceCLIFactory implements IFactory {

    private final File appsDir;

    public KustomTraceCLIFactory(File appsDir) {
        this.appsDir = appsDir;
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        if (cls == AffectedAppsCommand.class) {
            AffectedAppsCommand command = new AffectedAppsCommand();
            command.setAppsDir(appsDir);
            return (K) command;
        } else if (cls == AppFilesCommand.class) {
            AppFilesCommand command = new AppFilesCommand();
            command.setAppsDir(appsDir);
            return (K) command;
        }
        // For other commands, use the default factory
        return CommandLine.defaultFactory().create(cls);
    }
}