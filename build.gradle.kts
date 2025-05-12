plugins {
    id("java")
    id("com.diffplug.spotless") version "7.0.3"
}

group = "dev.zucca-ops"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

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