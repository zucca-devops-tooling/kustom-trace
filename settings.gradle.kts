rootProject.name = "kustomtrace"
include("lib")
include("cli")
project(":lib").name = rootProject.name
project(":cli").name = rootProject.name + "-cli"