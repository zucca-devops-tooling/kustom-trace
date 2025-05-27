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

tasks.register("tagRelease") {
    group = "release"
    description = "Tags the current version from build.gradle.kts in Git"

    doLast {
        val version = project.version.toString()
        val tagName = "v$version"

        exec {
            commandLine("git", "tag", "-a", tagName, "-m", "Release $tagName")
        }
    }
}