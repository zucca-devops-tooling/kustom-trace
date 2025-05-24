/*
 * Copyright 2025 GuidoZuccarelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zucca_ops.kustomtrace.cli;

import dev.zucca_ops.kustomtrace.cli.commands.AffectedAppsCommand;
import dev.zucca_ops.kustomtrace.cli.commands.AppFilesCommand;
import dev.zucca_ops.kustomtrace.cli.commands.ListRootAppsCommand;
import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "kustomtrace",
        version = "1.0.0",
        mixinStandardHelpOptions = true,
        description = "Analyzes Kubernetes deployment repositories.",
        subcommands = {AffectedAppsCommand.class, AppFilesCommand.class, ListRootAppsCommand.class})
public class KustomTraceCLI implements Callable<Integer> {

    @Option(
            names = {"--log-level"},
            description =
                    "Set log level: ERROR, WARN, INFO, DEBUG, TRACE. Default: WARN (console), INFO (file).")
    String logLevel;

    @Option(
            names = {"-v", "--version"},
            versionHelp = true,
            description = "Print version information and exit.")
    boolean versionRequested;

    @Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "Display this help message and exit.")
    boolean usageHelpRequested;

    @Option(
            names = {"-a", "--apps-dir"},
            required = true,
            description = "Path to the root apps directory.")
    File appsDir;

    @Option(
            names = {"--log-file"},
            paramLabel = "<file>",
            description = "Specify a file to write warnings and errors to.")
    File logFile;


    @Option(
            names = {"-o", "--output"},
            paramLabel = "<file>",
            description = "Output the list of affected apps to the specified YAML file.")
    private File outputFile;

    public File getAppsDir() {
        return appsDir;
    }

    public File getLogFile() {
        return logFile;
    }

    public File getOutputFile() {
        return outputFile;
    }

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    public static void main(String[] args) {
        String cmdLogFile = null;
        String cmdLogLevel = null;

        // Minimal pre-scan for --log-file and --log-level
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--log-file") && i + 1 < args.length) {
                cmdLogFile = args[++i];
            } else if (args[i].startsWith("--log-file=")) {
                cmdLogFile = args[i].substring("--log-file=".length());
            } else if (args[i].equals("--log-level") && i + 1 < args.length) {
                cmdLogLevel = args[++i];
            } else if (args[i].startsWith("--log-level=")) {
                cmdLogLevel = args[i].substring("--log-level=".length());
            }
        }

        // Set system properties for Logback BEFORE Picocli execution
        if (cmdLogFile != null && !cmdLogFile.trim().isEmpty()) {
            System.setProperty("LOG_FILE", new File(cmdLogFile).getAbsolutePath());
        }

        if (cmdLogLevel != null && !cmdLogLevel.trim().isEmpty()) {
            String levelUpper = cmdLogLevel.toUpperCase();
            System.setProperty("kustomtrace.app.loglevel", levelUpper);    // For your app's logger generation level
            System.setProperty("kustomtrace.console.loglevel", levelUpper); // For STDOUT ThresholdFilter
            System.setProperty("kustomtrace.file.loglevel", levelUpper);    // For FILE ThresholdFilter
        }
        // If --log-file is set but --log-level is not, console defaults to WARN (from logback.xml DEFAULT_CONSOLE_LEVEL)
        // and file defaults to INFO (from DEFAULT_FILE_LEVEL), app logs at DEFAULT_APP_LOG_LEVEL (INFO).
        // If neither is set, console gets WARN, app generates INFO (filtered by console's WARN). This achieves goal 2b.

        // Execute Picocli
        int exitCode = new CommandLine(new KustomTraceCLI())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }
}
