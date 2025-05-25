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
package dev.zucca_ops.kustomtrace.cli.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class CLIHelper {

    public static List<File> readFilesFromFile(File filesFromFile) throws IOException {
        List<File> files = new ArrayList<>();
        if (filesFromFile != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(filesFromFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        files.add(new File(line.trim()));
                    }
                }
            }
        }
        return files;
    }

    public static void printOutput(String header, List<?> items, File outputFile)
            throws FileNotFoundException {
        PrintWriter writer = null;
        if (outputFile != null) {
            writer = new PrintWriter(outputFile);
            writer.println(header);
            for (Object item : items) {
                writer.println("  - " + item);
            }
            writer.close();
            System.out.println("Output written to: " + outputFile.getAbsolutePath());
        } else {
            System.out.println(header);
            for (Object item : items) {
                System.out.println("  - " + item);
            }
        }
    }

    public static void printError(
            String message, File commandOutputOptFile, File dedicatedErrorLogFile) {
        String fullMessage = "Error: " + message;
        if (dedicatedErrorLogFile != null) {
            try (PrintWriter writer =
                    new PrintWriter(
                            new FileWriter(dedicatedErrorLogFile, true))) { // Append to error log
                writer.println(fullMessage);
            } catch (IOException e) {
                System.err.println(
                        "PANIC: Could not write to dedicated error log file: "
                                + dedicatedErrorLogFile
                                + " - "
                                + e.getMessage());
                System.err.println("Original error: " + fullMessage); // Fallback for original error
            }
        } else if (commandOutputOptFile != null) { // If -o is given and no dedicated error log
            try (PrintWriter writer =
                    new PrintWriter(
                            new FileWriter(
                                    commandOutputOptFile,
                                    true))) { // Append or overwrite based on desired behavior
                writer.println(fullMessage);
            } catch (
                    IOException
                            e) { // Changed from FileNotFoundException to IOException for FileWriter
                System.err.println(
                        "Error writing error to command output file: "
                                + commandOutputOptFile.getAbsolutePath()
                                + ": "
                                + e.getMessage());
                System.err.println("Original error: " + fullMessage);
            }
        } else {
            System.err.println(fullMessage);
        }
    }

    // For general warning messages
    public static void printWarning(String message, File dedicatedLogFile) {
        String fullMessage = "Warning: " + message;
        if (dedicatedLogFile != null) {
            try (PrintWriter writer =
                    new PrintWriter(new FileWriter(dedicatedLogFile, true))) { // Append
                writer.println(fullMessage);
            } catch (IOException e) {
                System.err.println(
                        "PANIC: Could not write warning to dedicated log file: "
                                + dedicatedLogFile
                                + " - "
                                + e.getMessage());
                System.err.println("Original warning: " + fullMessage); // Fallback
            }
        } else {
            System.err.println(fullMessage); // Default to System.err if no log file
        }
    }

    // For logging raw messages (like multi-line details or pre-formatted strings)
    public static void logRawMessage(String message, File dedicatedLogFile) {
        if (dedicatedLogFile != null) {
            try (PrintWriter writer =
                    new PrintWriter(new FileWriter(dedicatedLogFile, true))) { // Append
                writer.println(message);
            } catch (IOException e) {
                System.err.println(
                        "PANIC: Could not write raw message to dedicated log file: "
                                + dedicatedLogFile
                                + " - "
                                + e.getMessage());
                // Optionally print the raw message to System.err as fallback if critical
                // System.err.println("Original raw message: " + message);
            }
        } else {
            System.out.println(
                    message); // Default to System.out for general raw messages if no log file
        }
    }

    // For logging exception stack traces
    public static void logStackTrace(Exception e, File dedicatedLogFile) {
        if (dedicatedLogFile != null) {
            try (PrintWriter writer =
                    new PrintWriter(new FileWriter(dedicatedLogFile, true))) { // Append
                e.printStackTrace(writer);
            } catch (IOException ioe) {
                System.err.println(
                        "PANIC: Could not write stack trace to dedicated log file: "
                                + dedicatedLogFile
                                + " - "
                                + ioe.getMessage());
                e.printStackTrace(System.err); // Fallback to System.err
            }
        } else {
            e.printStackTrace(System.err); // Default to System.err if no log file
        }
    }

    /**
     * Writes data as YAML to the specified output file using SnakeYAML.
     */
    public static void writeYamlToFile(Object data, File outputFile) throws IOException {
        if (outputFile == null) {
            // This method is specifically for file output.
            // If outputFile is null, it implies an issue with how it was called.
            // Consider throwing an IllegalArgumentException or simply returning.
            System.err.println(
                    "CLIHelper.writeYamlToFile called with a null outputFile. No action taken.");
            return;
        }

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(
                DumperOptions.FlowStyle.BLOCK); // Prefer block style over inline/flow style
        options.setPrettyFlow(true); // For collections, enhance readability
        options.setIndent(2); // Standard 2-space indentation for mappings
        options.setIndicatorIndent(2); // Indentation for sequence '-' indicators
        options.setIndentWithIndicator(true); // Indent sequence items relative to the '-' indicator
        // options.setExplicitStart(true);  // Uncomment if you want the "---" document start marker

        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(outputFile)) {
            yaml.dump(data, writer);
        }
        // No "Output written to..." console message for clean YAML file output
    }
}
