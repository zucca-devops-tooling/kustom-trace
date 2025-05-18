plugins {
    id("java")
}

group = "dev.zucca-ops"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("info.picocli:picocli:4.7.7")
    implementation(project(":kustomtrace"))
    implementation("ch.qos.logback:logback-classic:1.5.3")
}

tasks.test {
    useJUnitPlatform()
}