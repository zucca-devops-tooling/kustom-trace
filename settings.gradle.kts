pluginManagement {
    repositories {
        // Temporary override for testing the publisher snapshot from GitHub Packages.
        maven {
            val githubPackagesUsername =
                providers.gradleProperty("githubPackagesUsername").orNull
                    ?: providers.environmentVariable("GH_PACKAGES_USERNAME").orNull
                    ?: providers.environmentVariable("GH_CREDENTIALS_USR").orNull
                    ?: providers.environmentVariable("GITHUB_ACTOR").orNull
            val githubPackagesPassword =
                providers.gradleProperty("githubPackagesPassword").orNull
                    ?: providers.environmentVariable("GH_PACKAGES_TOKEN").orNull
                    ?: providers.environmentVariable("GH_CREDENTIALS_PSW").orNull
                    ?: providers.environmentVariable("GITHUB_TOKEN").orNull

            name = "ZuccaGitHubPackages"
            url = uri("https://maven.pkg.github.com/zucca-devops-tooling/gradle-publisher")
            credentials {
                username = githubPackagesUsername
                password = githubPackagesPassword
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "kustomtrace"
include("lib")
include("cli")
project(":lib").name = rootProject.name
project(":cli").name = rootProject.name + "-cli"
include("functional-test")

