package util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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

    public static void printOutput(String header, List<?> items, File outputFile) throws FileNotFoundException {
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

    public static void printError(String message, File outputFile) {
        if (outputFile != null) {
            try (PrintWriter writer = new PrintWriter(outputFile)) {
                writer.println("Error: " + message);
            } catch (FileNotFoundException e) {
                System.err.println("Error writing error to file: " + outputFile.getAbsolutePath() + ": " + e.getMessage());
                System.err.println("Original error: " + message);
            }
        } else {
            System.err.println("Error: " + message);
        }
    }

    public static PrintWriter getPrintWriter(File outputFile) throws FileNotFoundException {
        if (outputFile != null) {
            return new PrintWriter(outputFile);
        }
        return null;
    }

    public static void closeWriter(PrintWriter writer, String successMessage, File outputFile) {
        if (writer != null) {
            writer.close();
            if (successMessage != null && outputFile != null) {
                System.out.println(successMessage + outputFile.getAbsolutePath());
            }
        }
    }
}
