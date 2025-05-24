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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
        name = "list-root-apps",
        mixinStandardHelpOptions = true,
        description =
                "Lists all root Kustomize applications (e.g., directories not referenced by other kustomizations).")
public class ListRootAppsCommand implements Callable<Integer> {

    @ParentCommand private KustomTraceCLI parentCLI;

    @Override
    public Integer call() {
        File effectiveAppsDir = parentCLI.getAppsDir();
        File effectiveLogFile = parentCLI.getLogFile();
        File outputFile = parentCLI.getOutputFile();

        // 1. Initial Validations for --apps-dir
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

        // 2. Core Logic
        try {
            KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDirPathGlobal);

            // Get the root application paths from KustomTrace (these are likely absolute)
            List<Path> absoluteRootAppPaths = kustomTrace.getRootApps();

            // Convert to paths relative to appsDirPathGlobal for display and YAML
            final File finalEffectiveLogFile = effectiveLogFile; // For use in lambda
            List<String> rootAppDisplayPaths =
                    absoluteRootAppPaths.stream()
                            .map(
                                    absoluteAppPath ->
                                            PathUtil.getRelativePath(
                                                    absoluteAppPath,
                                                    appsDirPathGlobal,
                                                    finalEffectiveLogFile))
                            .sorted() // Sort for consistent output order
                            .toList();

            // 3. Conditional Output
            if (outputFile != null) {
                // YAML output
                Map<String, List<String>> yamlOutput = new LinkedHashMap<>();
                yamlOutput.put("root-apps", rootAppDisplayPaths);
                CLIHelper.writeYamlToFile(yamlOutput, outputFile);
            } else {
                // Verbose console output
                if (rootAppDisplayPaths.isEmpty()) {
                    System.out.println("No root applications found in: " + appsDirPathGlobal);
                } else {
                    CLIHelper.printOutput(
                            "Root Applications:",
                            rootAppDisplayPaths,
                            null);
                }
            }
            return 0;

        } catch (Exception e) {
            String unexpectedUserMessage =
                    "An unexpected error occurred while listing root apps. Please check logs for details.";
            CLIHelper.logRawMessage(
                    "UNEXPECTED ERROR in list-root-apps: " + e.getMessage(), effectiveLogFile);
            CLIHelper.logStackTrace(e, effectiveLogFile);
            CLIHelper.printError(unexpectedUserMessage, null, effectiveLogFile);
            return 1;
        }
    }
}
