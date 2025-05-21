package dev.zucca_ops.kustomtrace.cli.util;


import java.io.File;
import java.nio.file.Path;

public class PathUtil {
    public static String getRelativePath(Path targetPath, Path basePath, File logFileForWarnings) {
        String pathString;
        try {
            Path absoluteTargetPath = targetPath.toAbsolutePath().normalize();
            Path absoluteBasePath = basePath.toAbsolutePath().normalize();

            if (absoluteTargetPath.startsWith(absoluteBasePath)) {
                pathString = absoluteBasePath.relativize(absoluteTargetPath).toString();
            } else {
                pathString = absoluteTargetPath.toString();
            }
        } catch (IllegalArgumentException e) {
            String warningDetail = "Could not reliably relativize path '" + targetPath +
                    "' against base '" + basePath + "'. Using path as is. Detail: " + e.getMessage();

            CLIHelper.printWarning(warningDetail, logFileForWarnings);
            pathString = targetPath.toAbsolutePath().normalize().toString(); // Fallback
        }

        // Normalize to forward slashes for consistent YAML output
        return pathString.replace(File.separator, "/");
    }
}