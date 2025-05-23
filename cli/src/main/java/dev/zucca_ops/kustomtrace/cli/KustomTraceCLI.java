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
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "kustomtrace", version = "1.0.0",
        mixinStandardHelpOptions = true,
        description = "Analyzes Kubernetes deployment repositories.",
        subcommands = {
                AffectedAppsCommand.class,
                AppFilesCommand.class,
                ListRootAppsCommand.class
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

        public File getAppsDir() {
                return appsDir;
        }

        public File getLogFile() {
                return logFile;
        }

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
                int exitCode = new CommandLine(new KustomTraceCLI()) // No custom factory needed here
                        .setCaseInsensitiveEnumValuesAllowed(true) // Optional: Example of other configurations
                        .execute(args);
                System.exit(exitCode);
        }
}