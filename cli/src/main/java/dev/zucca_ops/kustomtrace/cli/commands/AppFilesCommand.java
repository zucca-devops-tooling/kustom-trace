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
package dev.zucca_ops.kustomtrace.cli.commands;

import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.cli.KustomTraceCLI;
import dev.zucca_ops.kustomtrace.cli.util.CLIHelper;
import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "app-files",
        mixinStandardHelpOptions = true,
        description =
                "Lists all files used by a given application directory or kustomization file.")
public class AppFilesCommand implements Callable<Integer> {
    @ParentCommand private KustomTraceCLI parentCLI;

    @Parameters(
            arity = "1",
            paramLabel = "<app-path>",
            description = "Path to the application directory or its kustomization.yaml file.")
    File appPathInput;

    @Override
    public Integer call() {
        File effectiveAppsDir = parentCLI.getAppsDir();
        File effectiveLogFile = parentCLI.getLogFile();
        File outputFile = parentCLI.getOutputFile();

        // 1. Initial Validations for global --apps-dir
        if (effectiveAppsDir == null) {
            CLIHelper.printError(
                    "Critical: --apps-dir was not properly configured.", null, effectiveLogFile);
            return 1;
        }
        final Path appsDirPathGlobal = effectiveAppsDir.toPath().toAbsolutePath().normalize();
        if (!Files.isDirectory(appsDirPathGlobal)) {
            CLIHelper.printError(
                    "Invalid --apps-dir (not a directory or does not exist): " + appsDirPathGlobal,
                    null,
                    effectiveLogFile);
            return 1;
        }

        // 2. Validation for <app-path> input
        if (appPathInput == null) { // appPathInput is the @Parameters File
            CLIHelper.printError("<app-path> parameter is required.", null, effectiveLogFile);
            return 1;
        }

        Path appPathInputAsPath = appPathInput.toPath();
        Path appPathInputAbsolute = appPathInputAsPath.toAbsolutePath().normalize();

        if (!Files.exists(appPathInputAbsolute)) {
            CLIHelper.printError(
                    "Invalid <app-path> (path does not exist): " + appPathInputAbsolute,
                    null,
                    effectiveLogFile);
            return 1;
        }

        // Determine the application's effective directory for output formatting.
        // KustomTrace will handle the actual resolution for its processing.
        final Path appDirectoryForOutputFormatting;
        if (Files.isDirectory(appPathInputAbsolute)) {
            appDirectoryForOutputFormatting = appPathInputAbsolute;
        } else if (Files.isRegularFile(appPathInputAbsolute)) {
            // If it's a file, its parent is the directory for relativization context.
            // KustomTrace will later validate if it's a *proper* kustomization file.
            Path parent = appPathInputAbsolute.getParent();
            if (parent == null) {
                CLIHelper.printError(
                        "Invalid <app-path>: File path has no parent directory: "
                                + appPathInputAbsolute,
                        null,
                        effectiveLogFile);
                return 1;
            }
            appDirectoryForOutputFormatting = parent.toAbsolutePath().normalize();
        } else {
            CLIHelper.printError(
                    "Invalid <app-path>: Path is not a regular file or directory: "
                            + appPathInputAbsolute,
                    null,
                    effectiveLogFile);
            return 1;
        }

        // 3. Core Logic
        try {
            KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDirPathGlobal);

            // KustomTrace.getDependenciesFor is responsible for resolving appPathInputAsPath
            // (whether it's a dir or direct kustomization file) to the actual kustomization file.
            List<Path> dependencies = kustomTrace.getDependenciesFor(appPathInputAsPath);

            // Prepare appIdentifierKey for YAML and console header
            final String appIdentifierKey;
            Path relativePathFromAppsDir =
                    appsDirPathGlobal.relativize(appDirectoryForOutputFormatting);
            if (relativePathFromAppsDir.toString().isEmpty()) {
                Path namePart = appDirectoryForOutputFormatting.getFileName();
                appIdentifierKey = (namePart == null) ? "" : namePart.toString();
            } else {
                appIdentifierKey = relativePathFromAppsDir.toString();
            }
            final String finalAppIdentifierKey = appIdentifierKey.replace(File.separator, "/");

            // Prepare relativeDependencyPaths for output
            final File finalEffectiveLogFile = effectiveLogFile; // for use in lambda
            List<String> relativeDependencyPaths =
                    dependencies.stream()
                            .map(
                                    dependencyPath -> {
                                        Path absoluteDependencyPath =
                                                dependencyPath.toAbsolutePath().normalize();
                                        String pathStr;
                                        try {
                                            // Relativize dependencyPath against the app's own
                                            // directory
                                            pathStr =
                                                    appDirectoryForOutputFormatting
                                                            .relativize(absoluteDependencyPath)
                                                            .toString();
                                        } catch (IllegalArgumentException e) {
                                            // Fallback if not under appDirectory (e.g., different
                                            // root).
                                            String fallbackPathString;
                                            try {
                                                fallbackPathString =
                                                        appsDirPathGlobal
                                                                .relativize(absoluteDependencyPath)
                                                                .toString();
                                                CLIHelper.printWarning(
                                                        String.format(
                                                                "Dependency '%s' for app '%s' is not under its app directory '%s'. Using path relative to main apps dir: '%s'.",
                                                                absoluteDependencyPath,
                                                                finalAppIdentifierKey,
                                                                appDirectoryForOutputFormatting,
                                                                fallbackPathString),
                                                        finalEffectiveLogFile);
                                            } catch (IllegalArgumentException e2) {
                                                fallbackPathString =
                                                        absoluteDependencyPath
                                                                .toString(); // Absolute path as
                                                // last resort
                                                CLIHelper.printWarning(
                                                        String.format(
                                                                "Dependency '%s' for app '%s' could not be made relative to its app directory ('%s') or main apps dir. Using absolute path: '%s'.",
                                                                absoluteDependencyPath,
                                                                finalAppIdentifierKey,
                                                                appDirectoryForOutputFormatting,
                                                                fallbackPathString
                                                                ),
                                                        finalEffectiveLogFile);
                                            }
                                            pathStr = fallbackPathString;
                                        }
                                        return pathStr.replace(File.separator, "/");
                                    })
                            .sorted()
                            .collect(Collectors.toList());

            // 4. Conditional Output
            if (outputFile != null) {
                Map<String, List<String>> appData = new LinkedHashMap<>();
                appData.put(finalAppIdentifierKey, relativeDependencyPaths);
                Map<String, Object> yamlRoot = new LinkedHashMap<>();
                yamlRoot.put("app-files", appData);
                CLIHelper.writeYamlToFile(
                        yamlRoot, outputFile);
            } else {
                String consoleHeader = "Files used by application '" + finalAppIdentifierKey + "':";
                CLIHelper.printOutput(
                        consoleHeader, relativeDependencyPaths, null);
            }
            return 0;

        } catch (NotAnAppException e) {
            CLIHelper.printError(
                    "Invalid <app-path> '"
                            + appPathInput
                            + "': Not recognized as a Kustomize application. "
                            + e.getMessage(),
                    null,
                    effectiveLogFile);
            return 1;
        } catch (KustomException e) {
            CLIHelper.printError(
                    "Error processing application " + appPathInput + ": " + e.getMessage(),
                    null,
                    effectiveLogFile);
            return 1;
        } catch (Exception e) {
            String unexpectedUserMessage =
                    "An unexpected error occurred processing app-files for "
                            + appPathInput
                            + ". Please check logs for details.";
            CLIHelper.logRawMessage(
                    "UNEXPECTED ERROR in app-files: " + e.getMessage(), effectiveLogFile);
            CLIHelper.logStackTrace(e, effectiveLogFile);
            CLIHelper.printError(unexpectedUserMessage, null, effectiveLogFile);
            return 1;
        }
    }
}
