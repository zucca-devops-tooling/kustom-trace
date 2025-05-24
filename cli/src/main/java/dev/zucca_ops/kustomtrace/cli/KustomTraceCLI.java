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

    public File getAppsDir() {
        return appsDir;
    }

    public File getLogFile() {
        return logFile;
    }

    @Override
    public Integer call() {
        new CommandLine(this).usage(System.out);
        return 0;
    }

    public static void main(String[] args) {
        String logFileArgValue = null;
        String logLevelArgValue = null;

        // Find only --log-file and --log-level before Picocli parsing
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("--log-file")) {
                if (args[i].contains("=")) {
                    logFileArgValue = args[i].substring(args[i].indexOf("=") + 1);
                } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    logFileArgValue = args[i + 1]; // Value is next argument
                }
            } else if (args[i].startsWith("--log-level")) {
                if (args[i].contains("=")) {
                    logLevelArgValue = args[i].substring(args[i].indexOf("=") + 1);
                } else if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                    logLevelArgValue = args[i + 1]; // Value is next argument
                }
            }
        }

        // Set LOG_FILE property if --log-file was found
        if (logFileArgValue != null && !logFileArgValue.trim().isEmpty()) {
            System.setProperty("LOG_FILE", new File(logFileArgValue).getAbsolutePath());
            // If LOG_FILE is set, make console less verbose by default unless --log-level overrides
            if (logLevelArgValue == null) { // Only set this default if --log-level isn't also setting it
                System.setProperty("kustomtrace.console.loglevel", "ERROR");
            }
        }

        // Set log level properties if --log-level was found
        if (logLevelArgValue != null && !logLevelArgValue.trim().isEmpty()) {
            String levelUpper = logLevelArgValue.toUpperCase();
            System.setProperty("kustomtrace.app.loglevel", levelUpper);
            System.setProperty("kustomtrace.file.loglevel", levelUpper);    // Affects FILE appender's filter
            System.setProperty("kustomtrace.console.loglevel", levelUpper); // Affects STDOUT appender's filter
        }

        int exitCode = new CommandLine(new KustomTraceCLI())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }
}
