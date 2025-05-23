rootProject.name = "kustomtrace"
include("lib")
include("cli")
project(":lib").name = rootProject.name
project(":cli").name = rootProject.name + "-cli"
include("functional-test")

pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/zucca-devops-tooling/gradle-publisher")

            credentials {
                username = gradle.startParameter.projectProperties["githubPackagesUsername"]
                password = gradle.startParameter.projectProperties["githubPackagesPassword"]
            }
        }
        gradlePluginPortal()
    }
}
