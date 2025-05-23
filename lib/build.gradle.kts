plugins {
    id("java")
    id("dev.zucca-ops.gradle-publisher") version "1.0.4"
    id("signing")
}

group = "dev.zucca-ops"
version = rootProject.version

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.slf4j:slf4j-api:2.0.9")

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

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("KustomTrace Library")
            description.set("A Java library to analyze Kustomize configurations and build a dependency graph.")
            url.set("https://github.com/zucca-devops-tooling/kustom-trace/tree/main/lib")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("zucca")
                    name.set("Guido Zuccarelli")
                    email.set("guidozuccarelli@hotmail.com")
                    organizationUrl.set("https://github.com/zucca-devops-tooling")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/zucca-devops-tooling/kustom-trace.git")
                developerConnection.set("scm:git:ssh://github.com/zucca-devops-tooling/kustom-trace.git")
                url.set("https://github.com/zucca-devops-tooling/kustom-trace/")
            }
        }
    }
}

signing {
    val keyId = findProperty("signing.keyId") as String?
    val password = findProperty("signing.password") as String?
    val keyPath = findProperty("signing.secretKeyRingFile")?.toString()

    if (!keyId.isNullOrBlank() && !password.isNullOrBlank() && !keyPath.isNullOrBlank()) {
        logger.lifecycle("🔐 Using GPG secret key file at $keyPath")
        useInMemoryPgpKeys(File(keyPath).readText(), password)
        publishing.publications.withType<MavenPublication>().configureEach {
            signing.sign(this)
        }
    } else {
        logger.warn("🔐 File-based signing skipped: missing keyId, password, or key file")
    }
}

afterEvaluate {
    tasks.matching { it.name == "publishPluginMavenPublicationToLocalRepository" }.configureEach {
        dependsOn("signMavenPublication")
    }
    tasks.matching { it.name == "publishMavenPublicationToLocalRepository" }.configureEach {
        dependsOn("signPluginMavenPublication")
    }
}

publisher {
    prod {
        target = "mavenCentral"
        usernameProperty = "mavenCentralUsername"
        passwordProperty = "mavenCentralPassword"
    }
    dev {
        target = "https://maven.pkg.github.com/zucca-devops-tooling/kustom-trace"
        usernameProperty = "githubPackagesUsername"
        passwordProperty = "githubPackagesPassword"
        sign = false
    }
    gitFolder = "../"
    releaseBranchPatterns = listOf("PR-17")
}