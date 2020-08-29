plugins {
    id("org.jetbrains.intellij") version Versions.intellijPlugin
}

description = "IntelliJ platform plugin for Bowler."

dependencies {
    api(project(":util"))
    implementation(group = "io.arrow-kt", name = "arrow-core", version = Versions.arrow)
    implementation(group = "io.arrow-kt", name = "arrow-syntax", version = Versions.arrow)
    implementation(group = "io.arrow-kt", name = "arrow-optics", version = Versions.arrow)

    implementation(project(":logging"))

    testImplementation(project(":testUtil"))
}

intellij {
    version = Versions.intellijTarget
    setPlugins(
            "Groovy"
    )
}

tasks.publishPlugin {
    // Credit:
    // https://github.com/minecraft-dev/MinecraftDev/blob/e32a4a92085bf34690b3c3fd4c395f3b2be7bd95/build.gradle.kts#L126-L128
    properties["buildNumber"]?.let { buildNumber ->
        project.version = "${project.version}-$buildNumber"
    }
    token(System.getenv("INTELLIJ_PLUGIN_PUBLISH_TOKEN"))
}

tasks.patchPluginXml {
    version(project.version)
    sinceBuild(Versions.intellijSince)
    untilBuild(Versions.intellijUntil)
}
