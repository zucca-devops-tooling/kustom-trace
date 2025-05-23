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