plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("dev.zucca-ops.gradle-publisher") version "1.0.4-PR-42-SNAPSHOT"
    signing
}

group = "dev.zucca-ops"
version = rootProject.version

repositories {
    mavenCentral()
}

application {
    mainClass.set("dev.zucca_ops.kustomtrace.cli.KustomTraceCLI")
}

// Make the 'build' task depend on 'shadowJar' so the fat JAR is created during a build
tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.shadowJar {
    archiveBaseName.set("kustomtrace")
    archiveClassifier.set("all") // Creates 'kustomtrace-all.jar'
    manifest {
        // Access the mainClass from the application extension
        attributes(
            mapOf(
                "Main-Class" to project.extensions.getByType(org.gradle.api.plugins.JavaApplication::class.java).mainClass.get()
            )
        )
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("KustomTrace CLI")
            description.set("A Java CLI to analyze Kustomize repositories and get useful insights from it")
            url.set("https://github.com/zucca-devops-tooling/kustom-trace/tree/main/cli")
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

publisher {
    prod {
        target = "https://maven.pkg.github.com/zucca-devops-tooling/kustom-trace"
    }
    dev {
        target = "https://maven.pkg.github.com/zucca-devops-tooling/kustom-trace"
        sign = false
    }
    shadowJar = true
    usernameProperty = "githubPackagesUsername"
    passwordProperty = "githubPackagesPassword"
    gitFolder = "../"
}

dependencies {
    implementation("info.picocli:picocli:4.7.7")
    implementation(project(":"+rootProject.name))
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.yaml:snakeyaml:2.2")
}

tasks.test {
    useJUnitPlatform()
}