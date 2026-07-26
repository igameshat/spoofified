plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.minotaur)
}

group = "com.swaphat"
version = "2.1.1"

repositories {
    maven("https://maven.terraformersmc.com")
}

dependencies {
    minecraft(libs.minecraft)
    add("mappings", loom.officialMojangMappings())
    add("modImplementation", libs.fabric.loader)
    add("modImplementation", libs.fabric.api)
    add("modCompileOnly", libs.modmenu)
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
        archiveBaseName = "Spoofified-${libs.versions.minecraft.get()}"
    }
}