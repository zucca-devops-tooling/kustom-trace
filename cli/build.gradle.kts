plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("dev.zucca-ops.gradle-publisher") version "1.1.1"
    id("org.graalvm.buildtools.native") version "0.11.1"
}

group = "dev.zucca-ops"
version = rootProject.version

val picocliVersion = "4.7.7"

repositories {
    mavenCentral()
}

application {
    mainClass.set("dev.zucca_ops.kustomtrace.cli.KustomTraceCLI")
}

tasks.compileJava {
    options.compilerArgs.add("-Aproject=${project.group}/${project.name}")
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

tasks.shadowJar {
    archiveBaseName.set("kustomtrace-cli")
    archiveClassifier.set("all") // Creates 'kustomtrace-all.jar'
    manifest {
        // Access the mainClass from the application extension
        attributes(
            mapOf(
                "Main-Class" to project.extensions.getByType(JavaApplication::class.java).mainClass.get()
            )
        )
    }
}

graalvmNative {
    testSupport.set(false)
    metadataRepository {
        enabled.set(true)
    }
    binaries {
        named("main") {
            imageName.set("kustomtrace")
        }
    }
}

publisher {
    prod {
        target = "https://maven.pkg.github.com/zucca-devops-tooling/kustom-trace"
    }
    dev {
        target = "https://maven.pkg.github.com/zucca-devops-tooling/kustom-trace"
    }
    shadowJar = true
    usernameProperty = "githubPackagesUsername"
    passwordProperty = "githubPackagesPassword"
    gitFolder = "../"
}

dependencies {
    implementation("info.picocli:picocli:$picocliVersion")
    annotationProcessor("info.picocli:picocli-codegen:$picocliVersion")
    implementation(project(":"+rootProject.name))
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("org.codehaus.janino:janino:3.1.12")
}

tasks.test {
    useJUnitPlatform()
}
