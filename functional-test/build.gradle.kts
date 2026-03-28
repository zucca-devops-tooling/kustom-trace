import org.gradle.api.tasks.testing.Test

plugins {
    id("java")
}

group = "dev.zucca-ops"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":"+rootProject.name))
    implementation(project(":"+rootProject.name+"-cli"))

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("ch.qos.logback:logback-classic:1.5.32")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("info.picocli:picocli:4.7.7")
    testImplementation("org.yaml:snakeyaml:2.2")
}

tasks.test {
    useJUnitPlatform()
    exclude("nativecli/**")
}

tasks.register<Test>("nativeSmokeTest") {
    description = "Runs smoke tests against the native CLI binary."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    include("nativecli/**")
    shouldRunAfter(tasks.test)

    val nativeCliPath = providers.systemProperty("nativeCliPath").orElse("")
    inputs.property("nativeCliPath", nativeCliPath)
    systemProperty("nativeCliPath", nativeCliPath.get())
}
