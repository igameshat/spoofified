plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.minotaur)
}

group = "de.fabiexe"
version = "2.1.0"

repositories {
    maven("https://maven.terraformersmc.com")
}

dependencies {
    minecraft(libs.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    compileOnly(libs.modmenu)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }

    processResources {
        filesMatching("fabric.mod.json") {
            expand(mapOf(
                "version" to project.version,
                "minecraft_version" to libs.versions.minecraft.get(),
                "modmenu_version" to libs.versions.modmenu.get()
            ))
        }
    }

    jar {
        archiveBaseName = "ClientSpoofer-${libs.versions.minecraft.get()}"
    }
}