package dev.zucca_ops.kustomtrace.cli.commands;

import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.cli.KustomTraceCLI;
import dev.zucca_ops.kustomtrace.cli.util.CLIHelper;
import dev.zucca_ops.kustomtrace.cli.util.PathUtil;
import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "list-root-apps", mixinStandardHelpOptions = true,
        description = "Lists all root Kustomize applications (e.g., directories not referenced by other kustomizations).")
public class ListRootAppsCommand implements Callable<Integer> {

    @ParentCommand
    private KustomTraceCLI parentCLI;

    @Option(names = {"-o", "--output"}, paramLabel = "<file>",
            description = "Output the list of root applications to the specified YAML file.")
    private File outputFile;

    @Override
    public Integer call() throws Exception {
        File effectiveAppsDir = parentCLI.getAppsDir(); // Assuming public getter in KustomTraceCLI
        File effectiveLogFile = parentCLI.getLogFile(); // Assuming public getter

        // 1. Initial Validations for --apps-dir
        if (effectiveAppsDir == null) {
            CLIHelper.printError("Critical: --apps-dir was not properly configured.", null, effectiveLogFile);
            return 1;
        }
        final Path appsDirPathGlobal = effectiveAppsDir.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(appsDirPathGlobal)) {
            CLIHelper.printError("Invalid --apps-dir (not a directory or does not exist): " + appsDirPathGlobal, null, effectiveLogFile);
            return 1;
        }

        // 2. Core Logic
        try {
            KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDirPathGlobal);

            // Get the root application paths from KustomTrace (these are likely absolute)
            List<Path> absoluteRootAppPaths = kustomTrace.getRootApps();

            // Convert to paths relative to appsDirPathGlobal for display and YAML
            final File finalEffectiveLogFile = effectiveLogFile; // For use in lambda
            List<String> rootAppDisplayPaths = absoluteRootAppPaths.stream()
                    .map(absoluteAppPath -> PathUtil.getRelativePath(absoluteAppPath, appsDirPathGlobal, finalEffectiveLogFile))
                    .sorted() // Sort for consistent output order
                    .toList();

            // 3. Conditional Output
            if (this.outputFile != null) {
                // YAML output
                Map<String, List<String>> yamlOutput = new LinkedHashMap<>();
                yamlOutput.put("root-apps", rootAppDisplayPaths);
                CLIHelper.writeYamlToFile(yamlOutput, this.outputFile);
            } else {
                // Verbose console output
                if (rootAppDisplayPaths.isEmpty()) {
                    System.out.println("No root applications found in: " + appsDirPathGlobal);
                } else {
                    CLIHelper.printOutput("Root Applications:", rootAppDisplayPaths, null); // Using your 3-arg printOutput
                }
            }
            return 0; // Success

        } catch (Exception e) {
            String unexpectedUserMessage = "An unexpected error occurred while listing root apps. Please check logs for details.";
            CLIHelper.logRawMessage("UNEXPECTED ERROR in list-root-apps: " + e.getMessage(), effectiveLogFile);
            CLIHelper.logStackTrace(e, effectiveLogFile);
            CLIHelper.printError(unexpectedUserMessage, null, effectiveLogFile);
            return 1;
        }
    }
}