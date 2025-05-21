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
import dev.zucca_ops.kustomtrace.KustomTrace;
import dev.zucca_ops.kustomtrace.cli.util.CLIHelper;
import dev.zucca_ops.kustomtrace.cli.util.PathUtil;
import dev.zucca_ops.kustomtrace.exceptions.KustomException;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "app-files", mixinStandardHelpOptions = true,
        description = "Lists all files used by a given application.")
public class AppFilesCommand implements Callable<Integer> {

    @ParentCommand
    private KustomTraceCLI parentCLI;

    @Parameters(arity = "1", paramLabel = "<app-path>",
            description = "Path to the application's kustomization.yaml.")
    File appPath;

    @Option(names = {"-o", "--output"}, paramLabel = "<file>",
            description = "Output the list of files to the specified YAML file.")
    File outputFile; // This is specific to this command's successful output

    @Override
    public Integer call() throws Exception {
        File effectiveAppsDir = parentCLI.appsDir;
        File effectiveLogFile = parentCLI.logFile;

        // 1. Initial Validations
        if (effectiveAppsDir == null) {
            CLIHelper.printError("Critical: --apps-dir was not properly configured.", null, effectiveLogFile);
            return 1;
        }
        if (!effectiveAppsDir.exists() || !effectiveAppsDir.isDirectory()) {
            CLIHelper.printError("Invalid --apps-dir: " + effectiveAppsDir.getAbsolutePath(), null, effectiveLogFile);
            return 1;
        }
        // The arity="1" on @Parameters should make Picocli handle appPath == null,
        // but an explicit check here is defensive if Picocli's behavior changes or for direct calls.
        if (appPath == null) {
            CLIHelper.printError("<app-path> parameter is required.", null, effectiveLogFile);
            return 1; // Or exit code 2 to mimic Picocli
        }
        if (!appPath.exists() || !appPath.isFile()) {
            CLIHelper.printError("Invalid <app-path> (file does not exist or is not a file): " + appPath.getAbsolutePath(), null, effectiveLogFile);
            return 1;
        }

        // 2. Core Logic
        try {
            Path appsDirPath = effectiveAppsDir.toPath();
            KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDirPath);
            Path appFilePath = appPath.toPath().toAbsolutePath().normalize();

            List<Path> dependencies = kustomTrace.getDependenciesFor(appFilePath); // Can throw KustomException

            // Prepare relative paths for output consistency
            String relativeAppPathKey = PathUtil.getRelativePath(appFilePath, appsDirPath, effectiveLogFile);

            List<String> relativeDependencyPaths = dependencies.stream()
                    .map(p -> PathUtil.getRelativePath(p, appsDirPath, effectiveLogFile))
                    .collect(Collectors.toList());

            // 3. Conditional Output
            if (this.outputFile != null) {
                // YAML output to file
                Map<String, List<String>> appData = new LinkedHashMap<>();
                appData.put(relativeAppPathKey, relativeDependencyPaths);

                Map<String, Object> yamlRoot = new LinkedHashMap<>();
                yamlRoot.put("app-files", appData); // Root key for app-files command output

                CLIHelper.writeYamlToFile(yamlRoot, this.outputFile);
            } else {
                // Verbose output to console using your existing CLIHelper.printOutput
                String consoleHeader = "Files used by application at: " + relativeAppPathKey;
                CLIHelper.printOutput(consoleHeader, relativeDependencyPaths, null); // Pass null for outputFile
            }
            return 0; // Success

        } catch (KustomException e) {
            CLIHelper.printError(e.getMessage(), null, effectiveLogFile);
            return 1;
        } catch (Exception e) {
            String unexpectedUserMessage = "An unexpected error occurred processing app-files. Please check logs for details.";
            CLIHelper.logRawMessage("UNEXPECTED ERROR in app-files: " + e.getMessage(), effectiveLogFile);
            CLIHelper.logStackTrace(e, effectiveLogFile);
            CLIHelper.printError(unexpectedUserMessage, null, effectiveLogFile);
            return 1;
        }
    }
}