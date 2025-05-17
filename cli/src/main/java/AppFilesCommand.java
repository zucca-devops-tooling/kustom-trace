import exceptions.KustomException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import util.CLIHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "app-files", description = "Lists all files used by a given application.")
public class AppFilesCommand implements Callable<Integer> {
    @Parameters(arity = "1", paramLabel = "<app-path>", description = "Path to the application's kustomization.yaml.")
    private File appPath;

    @Option(names = {"-o", "--output"}, paramLabel = "<file>", description = "Output the list of files to the specified file.")
    private File outputFile;

    @Option(names = {"-a", "--apps-dir"}, required = true, description = "Path to the root apps directory.")
    private File appsDir;

    public void setAppsDir(File appsDir) {
        this.appsDir = appsDir;
    }

    @Override
    public Integer call() throws Exception {
        if (appPath == null) {
            CLIHelper.printError("<app-path> is required.", outputFile);
            return 1;
        }
        if (appsDir == null) {
            CLIHelper.printError("--apps-dir is required.", outputFile);
            return 1;
        }

        Path appsDirPath = appsDir.toPath();
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDirPath);
        Path appFilePath = appPath.toPath().toAbsolutePath().normalize();

        try {
            List<Path> dependencies = kustomTrace.getDependenciesFor(appFilePath);
            CLIHelper.printOutput("Files used by application at: " + appFilePath, dependencies, outputFile);
            return 0;

        } catch (KustomException e) {
            CLIHelper.printError(e.getMessage(), outputFile);
            return 1;
        }
    }
}