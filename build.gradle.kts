plugins {
    id("java")
    id("com.diffplug.spotless") version "7.0.3"
}

group = "dev.zucca-ops"
version = "1.0.0"

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/main/java/**/*.java")
            licenseHeaderFile(rootProject.file("config/license-header.txt"))
        }

        format("testJava") {
            target("src/test/java/**/*.java")
            java{ }
        }
    }
}

tasks.register("tagRelease") {
    group = "release"
    description = "Tags the current version from build.gradle.kts in Git"

    doLast {
        val version = project.version.toString()
        val tagName = "v$version"

        exec {
            commandLine("git", "tag", "-a", tagName, "-m", "Release $tagName")
        }
        exec {
            commandLine("git", "push", "origin", tagName)
        }
    }
}