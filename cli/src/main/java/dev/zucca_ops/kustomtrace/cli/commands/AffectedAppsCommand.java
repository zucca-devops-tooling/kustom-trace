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
import dev.zucca_ops.kustomtrace.cli.util.PathUtil;
import dev.zucca_ops.kustomtrace.exceptions.UnreferencedFileException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "affected-apps",
        mixinStandardHelpOptions = true,
        description = "Finds applications affected by changes in specified files.")
public class AffectedAppsCommand implements Callable<Integer> {

    @ParentCommand private KustomTraceCLI parentCLI;

    @Parameters(
            arity = "0..*",
            paramLabel = "<modified-file>",
            description = "One or more modified file paths.")
    private File[] modifiedFilesArray;

    @Option(
            names = {"-f", "--files-from-file"},
            paramLabel = "<file>",
            description = "Read modified file paths from the specified file (one path per line).")
    private File filesFromFile;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "<file>",
            description = "Output the list of affected apps to the specified YAML file.")
    private File outputFile; // For YAML output

    @Override
    public Integer call() throws Exception {
        File effectiveAppsDir = parentCLI.getAppsDir();
        File effectiveLogFile = parentCLI.getLogFile();

        // 1. Validations for appsDir (from parent)
        if (effectiveAppsDir == null) {
            CLIHelper.printError(
                    "Critical: --apps-dir was not properly configured.", null, effectiveLogFile);
            return 1;
        }
        if (!effectiveAppsDir.exists() || !effectiveAppsDir.isDirectory()) {
            CLIHelper.printError(
                    "Invalid --apps-dir: " + effectiveAppsDir.getAbsolutePath(),
                    null,
                    effectiveLogFile);
            return 1;
        }

        // 2. Gather all modified files
        List<File> allModifiedFiles = new ArrayList<>();
        if (this.modifiedFilesArray != null && this.modifiedFilesArray.length > 0) {
            allModifiedFiles.addAll(Arrays.asList(this.modifiedFilesArray));
        }
        try {
            if (this.filesFromFile != null) {
                if (!this.filesFromFile.exists()) {
                    CLIHelper.printError(
                            "File specified by --files-from-file not found: "
                                    + this.filesFromFile.getAbsolutePath(),
                            null,
                            effectiveLogFile);
                    return 1;
                }
                allModifiedFiles.addAll(CLIHelper.readFilesFromFile(this.filesFromFile));
            }
        } catch (IOException e) {
            CLIHelper.printError(
                    "Error reading files from --files-from-file: "
                            + (this.filesFromFile != null
                                    ? this.filesFromFile.getAbsolutePath()
                                    : "null")
                            + " - "
                            + e.getMessage(),
                    null,
                    effectiveLogFile);
            return 1;
        }

        if (allModifiedFiles.isEmpty()) {
            // If outputting to file, write an empty structure. If to console, print a message.
            if (this.outputFile != null) {
                Map<String, Object> yamlRoot = new LinkedHashMap<>();
                yamlRoot.put("affected-apps", new LinkedHashMap<>()); // Empty map of affected apps
                CLIHelper.writeYamlToFile(yamlRoot, this.outputFile);
            } else {
                System.out.println("Affected Applications:");
                System.out.println("  No modified files provided to check.");
            }
            return 0; // Not an error, just no input to process.
        }

        // 3. Core Logic & Data Preparation
        try {
            Path appsDirPath = effectiveAppsDir.toPath();
            KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDirPath);

            Map<String, List<String>> affectedAppsDataForYaml = new LinkedHashMap<>();
            List<String> consoleOutputLines = new ArrayList<>();
            boolean anyAppsAffectedOverall = false;

            if (this.outputFile == null) { // Main header for console output
                consoleOutputLines.add("Affected Applications:");
            }

            for (File modifiedFile : allModifiedFiles) {
                Path modifiedFileFullPath = modifiedFile.toPath().toAbsolutePath().normalize();
                String yamlKeyForModifiedFile;

                if (modifiedFileFullPath.startsWith(appsDirPath.toAbsolutePath().normalize())) {
                    // File is INSIDE appsDir: get path relative TO appsDir.
                    // PathUtil.getRelativePath should normalize to forward slashes.
                    yamlKeyForModifiedFile =
                            PathUtil.getRelativePath(
                                    modifiedFileFullPath, appsDirPath, effectiveLogFile);
                } else {
                    // File is OUTSIDE appsDir: use the path string as originally provided by the
                    // user,
                    // but normalize its separators to forward slashes.
                    yamlKeyForModifiedFile =
                            modifiedFile.getPath().replace(java.io.File.separator, "/");
                }

                List<String> relativeAffectedAppPathsForCurrentFile = new ArrayList<>();
                try {
                    List<Path> affectedApps = kustomTrace.getAppsWith(modifiedFileFullPath);

                    if (!affectedApps.isEmpty()) {
                        anyAppsAffectedOverall = true;
                    }

                    for (Path app : affectedApps) {
                        // PathUtil should correctly relativize app paths against appsDirPath
                        relativeAffectedAppPathsForCurrentFile.add(
                                PathUtil.getRelativePath(app, appsDirPath, effectiveLogFile));
                    }

                    Collections.sort(
                            relativeAffectedAppPathsForCurrentFile); // Sort alphabetically for
                    // consistent output

                    if (this.outputFile == null) { // Console output formatting for this file
                        consoleOutputLines.add(
                                "Affected apps by "
                                        + yamlKeyForModifiedFile
                                        + ":"); // Use yamlKeyForModifiedFile for console too
                        if (relativeAffectedAppPathsForCurrentFile.isEmpty()) {
                            consoleOutputLines.add("  - None");
                        } else {
                            relativeAffectedAppPathsForCurrentFile.forEach(
                                    appPath -> consoleOutputLines.add("  - " + appPath));
                        }
                    }
                } catch (UnreferencedFileException e) {
                    // relativeAffectedAppPathsForCurrentFile will remain empty
                    if (this.outputFile
                            == null) { // Console output mode for UnreferencedFileException
                        consoleOutputLines.add(
                                "Affected apps by "
                                        + yamlKeyForModifiedFile
                                        + ":"); // Use yamlKeyForModifiedFile
                        consoleOutputLines.add("  Warning: " + e.getMessage());
                    } else { // YAML output mode
                        if (effectiveLogFile != null) {
                            CLIHelper.printWarning(
                                    e.getMessage()
                                            + " (for modified file: "
                                            + yamlKeyForModifiedFile
                                            + ")",
                                    effectiveLogFile);
                        }
                    }
                }
                // Add to YAML map using the determined key
                affectedAppsDataForYaml.put(
                        yamlKeyForModifiedFile, relativeAffectedAppPathsForCurrentFile);
            }

            // 4. Conditional Output
            if (this.outputFile != null) {
                Map<String, Object> yamlRoot = new LinkedHashMap<>();
                yamlRoot.put("affected-apps", affectedAppsDataForYaml);
                CLIHelper.writeYamlToFile(yamlRoot, this.outputFile);
            } else {
                consoleOutputLines.forEach(
                        System.out::println); // Print all accumulated console lines
                if (!anyAppsAffectedOverall) {
                    System.out.println(
                            "Summary: No applications were found to be affected by the specified file(s).");
                }
            }
            return 0;
        } catch (Exception e) {
            String unexpectedUserMessage =
                    "An unexpected error occurred processing affected-apps. Please check logs for details.";
            CLIHelper.logRawMessage(
                    "UNEXPECTED ERROR in affected-apps: " + e.getMessage(), effectiveLogFile);
            CLIHelper.logStackTrace(e, effectiveLogFile);
            CLIHelper.printError(unexpectedUserMessage, null, effectiveLogFile);
            return 1;
        }
    }
}
