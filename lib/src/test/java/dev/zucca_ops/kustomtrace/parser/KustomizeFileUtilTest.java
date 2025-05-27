package dev.zucca_ops.kustomtrace.parser;

import dev.zucca_ops.kustomtrace.exceptions.NotAnAppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class KustomizeFileUtilTest {


    @TempDir
    Path tempDir; // Used for tests that interact with the file system

    // --- Tests for isFile(Path path) ---
    @Nested
    @DisplayName("isFile Tests")
    class IsFileTests {
        @Test
        void isFile_whenPathIsExistingRegularFile_shouldReturnTrue() throws IOException {
            Path existingFile = Files.createFile(tempDir.resolve("actual_file.txt"));
            assertTrue(KustomizeFileUtil.isFile(existingFile));
        }

        @Test
        void isFile_whenPathIsExistingDirectory_shouldReturnFalse() throws IOException {
            Path existingDir = Files.createDirectory(tempDir.resolve("actual_dir"));
            assertFalse(KustomizeFileUtil.isFile(existingDir));
        }

        @Test
        void isFile_whenPathDoesNotExist_shouldReturnFalse() {
            Path nonExistentPath = tempDir.resolve("non_existent.txt");
            assertFalse(KustomizeFileUtil.isFile(nonExistentPath));
        }

        @Test
        void isFile_whenPathIsNull_shouldThrowNullPointerException() {
            // Files.exists(null) throws NPE, so isFile(null) will also.
            assertThrows(NullPointerException.class, () -> KustomizeFileUtil.isFile(null));
        }
    }

    // --- Tests for isKustomizationFileName(Path path) ---
    @Nested
    @DisplayName("isKustomizationFileName Tests")
    class IsKustomizationFileNameTests {
        @Test
        void isKustomizationFileName_exactMatches_shouldReturnTrue() {
            assertTrue(KustomizeFileUtil.isKustomizationFileName(Paths.get("kustomization.yaml")));
            assertTrue(KustomizeFileUtil.isKustomizationFileName(Paths.get("kustomization.yml")));
            assertTrue(KustomizeFileUtil.isKustomizationFileName(Paths.get("Kustomization")));
            assertTrue(KustomizeFileUtil.isKustomizationFileName(Paths.get("some/dir/kustomization.yaml")));
        }

        @Test
        void isKustomizationFileName_incorrectNamesOrExtensions_shouldReturnFalse() {
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("Kustomization.yaml"))); // Different case
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("my-kustomization.yaml")));
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("kustomization.txt")));
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("some.yaml")));
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("kustomization"))); // Missing extension
        }

        @Test
        void isKustomizationFileName_pathWithNoFileName_shouldReturnFalse() {
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("/"))); // Root
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("C:\\"))); // Windows Root
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get(".")));
            assertFalse(KustomizeFileUtil.isKustomizationFileName(Paths.get("some/dir/"))); // Ends with separator
        }
    }

    // --- Tests for isValidKubernetesResource(Path path) ---
    @Nested
    @DisplayName("isValidKubernetesResource Tests")
    class IsValidKubernetesResourceTests {
        @Test
        void isValidKubernetesResource_validResourceFiles_shouldReturnTrue() {
            assertTrue(KustomizeFileUtil.isValidKubernetesResource(Paths.get("service.yaml")));
            assertTrue(KustomizeFileUtil.isValidKubernetesResource(Paths.get("deployment.yml")));
            assertTrue(KustomizeFileUtil.isValidKubernetesResource(Paths.get("configmap.json")));
            assertTrue(KustomizeFileUtil.isValidKubernetesResource(Paths.get("RESOURCE.YAML"))); // Case-insensitive ext
            assertTrue(KustomizeFileUtil.isValidKubernetesResource(Paths.get("some/dir/ingress.JSON")));
        }

        @Test
        void isValidKubernetesResource_kustomizationFiles_shouldReturnFalse() {
            assertFalse(KustomizeFileUtil.isValidKubernetesResource(Paths.get("kustomization.yaml")));
            assertFalse(KustomizeFileUtil.isValidKubernetesResource(Paths.get("kustomization.yml")));
            assertFalse(KustomizeFileUtil.isValidKubernetesResource(Paths.get("Kustomization")));
        }

        @Test
        void isValidKubernetesResource_invalidExtensions_shouldReturnFalse() {
            assertFalse(KustomizeFileUtil.isValidKubernetesResource(Paths.get("resource.txt")));
            assertFalse(KustomizeFileUtil.isValidKubernetesResource(Paths.get("resource.yam")));
        }

        @Test
        void isValidKubernetesResource_pathWithNoFileName_shouldReturnFalse() {
            assertFalse(KustomizeFileUtil.isValidKubernetesResource(Paths.get("/")));
            assertFalse(KustomizeFileUtil.isValidKubernetesResource(Paths.get(".")));
        }
    }

    // --- Tests for getKustomizationFileFromAppDirectory(Path path) ---
    @Nested
    @DisplayName("getKustomizationFileFromAppDirectory Tests")
    class GetKustomizationFileFromAppDirectoryTests {

        @Test
        void getKustomizationFile_inputIsExistingKustomizationYamlFile_returnsInputPath() throws IOException, NotAnAppException {
            Path kustomFile = Files.createFile(tempDir.resolve("kustomization.yaml"));
            assertEquals(kustomFile, KustomizeFileUtil.getKustomizationFileFromAppDirectory(kustomFile.getParent()));
        }

        @Test
        void getKustomizationFile_inputIsExistingKustomizationYmlFile_returnsInputPath() throws IOException, NotAnAppException {
            Path kustomFile = Files.createFile(tempDir.resolve("kustomization.yml"));
            assertEquals(kustomFile, KustomizeFileUtil.getKustomizationFileFromAppDirectory(kustomFile.getParent()));
        }

        @Test
        void getKustomizationFile_inputIsExistingKustomizationUpperFile_returnsInputPath() throws IOException, NotAnAppException {
            Path kustomFile = Files.createFile(tempDir.resolve("Kustomization"));
            assertEquals(kustomFile, KustomizeFileUtil.getKustomizationFileFromAppDirectory(kustomFile.getParent()));
        }

        @Test
        void getKustomizationFile_inputNameIsKustomization_butFileDoesNotExist_scansAsDirAndThrows(@TempDir Path tempDir) {
            Path appPath = tempDir.resolve("app"); // This directory itself is not created.
            Path pathNamedLikeKustomization = appPath.resolve("kustomization.yaml"); // This file is not created.

            // Logic: isKustomizationFileName(pathNamedLikeKustomization) -> true
            //        isFile(pathNamedLikeKustomization) -> false
            //        Fall-through: treat pathNamedLikeKustomization as directory to scan.
            //        Scan pathNamedLikeKustomization/kustomization.yaml etc. -> not found
            //        Throws NotAnAppException referring to pathNamedLikeKustomization
            NotAnAppException ex = assertThrows(NotAnAppException.class, () -> {
                KustomizeFileUtil.getKustomizationFileFromAppDirectory(pathNamedLikeKustomization);
            });
            assertTrue(ex.getMessage().contains(pathNamedLikeKustomization.toString()));
        }

        @Test
        void getKustomizationFile_inputNameIsKustomization_butPathIsDirectory_scansWithinThatDir(@TempDir Path tempDir) throws IOException, NotAnAppException {
            Path dirNamedAsKustomization = Files.createDirectory(tempDir.resolve("kustomization.yaml"));
            Path actualKustomFileInIt = Files.createFile(dirNamedAsKustomization.resolve("kustomization.yml")); // Actual file inside

            assertEquals(actualKustomFileInIt, KustomizeFileUtil.getKustomizationFileFromAppDirectory(dirNamedAsKustomization));
        }

        @Test
        void getKustomizationFile_inputNameIsKustomization_butPathIsDirectoryAndEmpty_throws(@TempDir Path tempDir) throws IOException {
            Path dirNamedAsKustomization = Files.createDirectory(tempDir.resolve("kustomization.yaml")); // Empty directory

            assertThrows(NotAnAppException.class, () -> {
                KustomizeFileUtil.getKustomizationFileFromAppDirectory(dirNamedAsKustomization);
            });
        }


        @Test
        void getKustomizationFile_inputIsDir_findsKustomizationYaml() throws IOException, NotAnAppException {
            Path appDir = Files.createDirectory(tempDir.resolve("myAppYaml"));
            Path kustomFile = Files.createFile(appDir.resolve("kustomization.yaml"));
            assertEquals(kustomFile, KustomizeFileUtil.getKustomizationFileFromAppDirectory(appDir));
        }

        @Test
        void getKustomizationFile_inputIsDir_findsKustomizationYml() throws IOException, NotAnAppException {
            Path appDir = Files.createDirectory(tempDir.resolve("myAppYml"));
            Path kustomFile = Files.createFile(appDir.resolve("kustomization.yml"));
            assertEquals(kustomFile, KustomizeFileUtil.getKustomizationFileFromAppDirectory(appDir));
        }

        @Test
        void getKustomizationFile_inputIsDir_findsKustomizationUpper() throws IOException, NotAnAppException {
            Path appDir = Files.createDirectory(tempDir.resolve("myAppUpperK"));
            Path kustomFile = Files.createFile(appDir.resolve("Kustomization"));
            assertEquals(kustomFile, KustomizeFileUtil.getKustomizationFileFromAppDirectory(appDir));
        }

        @Test
        void getKustomizationFile_inputIsDir_prefersYamlOverYmlAndUpper() throws IOException, NotAnAppException {
            Path appDir = Files.createDirectory(tempDir.resolve("appPrefersYaml"));
            Path kustomYaml = Files.createFile(appDir.resolve("kustomization.yaml"));
            Files.createFile(appDir.resolve("kustomization.yml"));
            Files.createFile(appDir.resolve("Kustomization"));
            assertEquals(kustomYaml, KustomizeFileUtil.getKustomizationFileFromAppDirectory(appDir));
        }

        @Test
        void getKustomizationFile_inputIsDir_prefersYmlOverUpper() throws IOException, NotAnAppException {
            Path appDir = Files.createDirectory(tempDir.resolve("appPrefersYml"));
            Path kustomYml = Files.createFile(appDir.resolve("kustomization.yml"));
            Files.createFile(appDir.resolve("Kustomization"));
            assertEquals(kustomYml, KustomizeFileUtil.getKustomizationFileFromAppDirectory(appDir));
        }

        @Test
        void getKustomizationFile_inputIsDir_isEmpty_throwsNotAnAppException() throws IOException {
            Path emptyDir = Files.createDirectory(tempDir.resolve("emptyActualAppDir"));
            assertThrows(NotAnAppException.class, () -> KustomizeFileUtil.getKustomizationFileFromAppDirectory(emptyDir));
        }

        @Test
        void getKustomizationFile_inputIsDir_containsOnlyOtherFiles_throwsNotAnAppException() throws IOException {
            Path appDir = Files.createDirectory(tempDir.resolve("appWithOnlyReadme"));
            Files.createFile(appDir.resolve("README.md"));
            assertThrows(NotAnAppException.class, () -> KustomizeFileUtil.getKustomizationFileFromAppDirectory(appDir));
        }

        @Test
        void getKustomizationFile_inputIsRegularFile_notKustomizationName_throwsNotAnAppException() throws IOException {
            Path regularFile = Files.createFile(tempDir.resolve("my-service.yaml"));
            assertThrows(NotAnAppException.class, () -> KustomizeFileUtil.getKustomizationFileFromAppDirectory(regularFile));
        }

        @Test
        void getKustomizationFile_inputPathDoesNotExist_throwsNotAnAppException() {
            Path nonExistentPath = tempDir.resolve("thisDirectoryDoesNotExist");
            assertThrows(NotAnAppException.class, () -> KustomizeFileUtil.getKustomizationFileFromAppDirectory(nonExistentPath));
        }
    }
}
