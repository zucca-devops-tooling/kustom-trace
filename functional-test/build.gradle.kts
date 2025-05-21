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
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("info.picocli:picocli:4.7.7")
    testImplementation("org.yaml:snakeyaml:2.2")
}

tasks.test {
    useJUnitPlatform()
}