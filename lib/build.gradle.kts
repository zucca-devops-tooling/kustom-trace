plugins {
    id("java")
    id("dev.zucca-ops.gradle-publisher") version "1.0.4"
}

group = "dev.zucca-ops"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    // Core dependencies
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.slf4j:slf4j-api:2.0.9")

    // Visualization
 //   implementation("org.graphstream:gs-core:2.0")
 //   implementation("org.graphstream:gs-ui:2.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

publisher {
    prod {
        //target = "mavenCentral"
        //usernameProperty = "mavenCentralUsername"
        //passwordProperty = "mavenCentralPassword"
        target = "https://maven.pkg.github.com/zucca-devops-tooling/kustom-trace"
        usernameProperty = "githubPackagesUsername"
        passwordProperty = "githubPackagesPassword"
    }
    dev {
        target = "https://maven.pkg.github.com/zucca-devops-tooling/kustom-trace"
        usernameProperty = "githubPackagesUsername"
        passwordProperty = "githubPackagesPassword"
        sign = false
    }

}