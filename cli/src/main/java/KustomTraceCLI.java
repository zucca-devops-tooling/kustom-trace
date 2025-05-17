import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "kustomtrace", version = "kustomtrace 1.0",
        description = "Analyzes Kubernetes deployment repositories.",
        subcommands = {
                AffectedAppsCommand.class,
                AppFilesCommand.class
        })
public class KustomTraceCLI implements Callable<Integer> {

        @Option(names = {"-v", "--version"}, versionHelp = true, description = "Print version information and exit.")
        boolean versionRequested;

        @Option(names = {"-h", "--help"}, usageHelp = true, description = "Display this help message and exit.")
        boolean usageHelpRequested;

        @Option(names = {"-a", "--apps-dir"}, required = true, description = "Path to the root apps directory.")
        File appsDir;

        @Option(names = {"--log-file"}, paramLabel = "<file>", description = "Specify a file to write warnings and errors to.")
        File logFile;

        @Override
        public Integer call() {
                if (appsDir == null) {
                        System.err.println("Error: --apps-dir is required for any operation.");
                        new CommandLine(this).usage(System.out);
                        return 1;
                }
                if (logFile != null) {
                        System.setProperty("log.file", logFile.getAbsolutePath());
                }
                new CommandLine(this).usage(System.out);
                return 0;
        }

        public static void main(String[] args) {
                KustomTraceCLI cli = new KustomTraceCLI();
                KustomTraceCLIFactory factory = new KustomTraceCLIFactory(cli.appsDir); // Removed logFile
                int exitCode = new CommandLine(cli, factory).execute(args);
                System.exit(exitCode);
        }
}