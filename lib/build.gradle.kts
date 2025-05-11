plugins {
    id("java")
}

group = "dev.zucca-ops"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Core dependencies
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Visualization
    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui:2.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}