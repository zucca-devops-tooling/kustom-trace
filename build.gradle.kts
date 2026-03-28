import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

plugins {
    id("java")
    id("com.diffplug.spotless") version "7.0.3"
}

group = "dev.zucca-ops"
version = "1.0.1"

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/main/java/**/*.java")
            googleJavaFormat("1.17.0").aosp().skipJavadocFormatting()
            licenseHeaderFile(rootProject.file("config/license-header.txt"))
        }

        format("testJava") {
            target("src/test/java/**/*.java")
            java{}
        }
    }
}

abstract class TagReleaseTask @Inject constructor(
    private val execOperations: ExecOperations
) : DefaultTask() {

    @TaskAction
    fun tagRelease() {
        val releaseVersion = project.version.toString()
        val tagName = "v$releaseVersion"

        execOperations.exec {
            commandLine("git", "tag", "-a", tagName, "-m", "Release $tagName")
        }
    }
}

tasks.register<TagReleaseTask>("tagRelease") {
    group = "release"
    description = "Tags the current version from build.gradle.kts in Git"
}
