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
            googleJavaFormat("1.17.0").aosp().skipJavadocFormatting()
            licenseHeaderFile(rootProject.file("config/license-header.txt"))
        }

        format("testJava") {
            target("src/test/java/**/*.java")
            java{}
        }
    }
}