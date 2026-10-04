plugins {
    `java-library`
    idea
    id("net.neoforged.moddev") version "2.0.147"
}


fun prop(name: String): String = providers.gradleProperty(name).get()

version = prop("mod_version")
group = prop("mod_group_id")

base {
    archivesName = prop("mod_id")
}

// Minecraft 26.3 ships Java 25 to players, so we target it too.
java.toolchain.languageVersion = JavaLanguageVersion.of(25)

sourceSets.main {
    resources {
        // Resources produced by `./gradlew runData`.
        srcDir("src/generated/resources")
        exclude("**/*.bbmodel")
        exclude(".cache")
    }
}

neoForge {
    version = prop("neo_version")

    runs {
        register("client") {
            client()
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id"))
        }
        register("server") {
            server()
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id"))
        }
        register("gameTestServer") {
            type = "gameTestServer"
            systemProperty("neoforge.enabledGameTestNamespaces", prop("mod_id"))
            // tools/structure-preview: -PpreviewDir=<dir> makes structure_preview_export write block lists there.
            providers.gradleProperty("previewDir").orNull?.let { systemProperty("strata.previewDir", it) }
            providers.gradleProperty("previewOnly").orNull?.let { systemProperty("strata.previewOnly", it) }
        }
        register("data") {
            clientData()
            programArguments.addAll(
                "--mod", prop("mod_id"),
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath,
            )
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(prop("mod_id")) {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    // JEI (recipe viewer). Compiled against its API only; the integration lives in compat/jei and
    // loads only when JEI is installed.
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com/") }
        filter {
            includeGroup("mezz.jei")
            includeGroupAndSubgroups("net.mezzdev")
        }
    }
}

// Optional runtime-only dependencies (e.g. recipe viewers) that we do not want to publish.
val localRuntime: Configuration by configurations.creating
configurations.runtimeClasspath {
    extendsFrom(localRuntime)
}

dependencies {
    compileOnly("mezz.jei:jei-${prop("minecraft_version")}-common-api:${prop("jei_version")}")
    compileOnly("mezz.jei:jei-${prop("minecraft_version")}-neoforge-api:${prop("jei_version")}")
}

// Expands ${...} placeholders in src/main/templates (neoforge.mods.toml).
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to prop("minecraft_version"),
        "minecraft_version_range" to prop("minecraft_version_range"),
        "neo_version" to prop("neo_version"),
        "mod_id" to prop("mod_id"),
        "mod_name" to prop("mod_name"),
        "mod_license" to prop("mod_license"),
        "mod_version" to prop("mod_version"),
        "mod_description" to prop("mod_description"),
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
