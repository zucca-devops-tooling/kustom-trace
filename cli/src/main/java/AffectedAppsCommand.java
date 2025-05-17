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
import exceptions.UnreferencedFileException;
import model.Kustomization;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import util.CLIHelper;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@Command(name = "affected-apps", description = "Finds applications affected by changes in specified files.")
public class AffectedAppsCommand implements Callable<Integer> {
    @Parameters(arity = "0..*", paramLabel = "<modified-file>", description = "One or more modified file paths.")
    private File[] modifiedFiles;

    @Option(names = {"-f", "--files-from-file"}, paramLabel = "<file>", description = "Read modified file paths from the specified file (one path per line).")
    private File filesFromFile;

    @Option(names = {"-o", "--output"}, paramLabel = "<file>", description = "Output the list of affected apps to the specified file.")
    private File outputFile;

    private File appsDir;

    public void setAppsDir(File appsDir) {
        this.appsDir = appsDir;
    }

    @Override
    public Integer call() throws Exception {
        List<File> allModifiedFiles = new ArrayList<>();
        if (modifiedFiles != null) {
            allModifiedFiles.addAll(Arrays.asList(modifiedFiles));
        }
        try {
            allModifiedFiles.addAll(CLIHelper.readFilesFromFile(filesFromFile));
        } catch (IOException e) {
            CLIHelper.printError("Error reading files from file: " + filesFromFile.getAbsolutePath(), outputFile);
            return 1;
        }

        if (allModifiedFiles.isEmpty()) {
            CLIHelper.printError("No modified files provided.", outputFile);
            return 1;
        }

        System.out.println("Finding affected apps for: " + allModifiedFiles.stream().map(File::getAbsolutePath).collect(Collectors.toList()) + " in " + appsDir.getAbsolutePath());

        Path appsDirPath = appsDir.toPath();
        KustomTrace kustomTrace = KustomTrace.fromDirectory(appsDirPath);
        List<String> outputLines = new ArrayList<>();

        for (File modifiedFile : allModifiedFiles) {
            Path modifiedFilePath = modifiedFile.toPath().toAbsolutePath().normalize();
            try {
                List<Kustomization> affected = kustomTrace.getAppsWith(modifiedFilePath);
                outputLines.add("Affected apps by " + modifiedFilePath + ":");
                affected.forEach(app -> outputLines.add("  - " + app.getPath()));
            } catch (UnreferencedFileException e) {
                outputLines.add("Warning: " + e.getMessage());
            }
        }

        CLIHelper.printOutput("Affected Applications:", outputLines, outputFile);

        return 0;
    }
}