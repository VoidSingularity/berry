plugins {
    id("net.mcberry.stem") version "1.0.1"
    id("maven-publish")
    id("java-library")
}

repositories {
    mavenLocal()
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
    withJavadocJar()
}

val asm_version: String by project
val mixins_version: String by project
val mixinextras_version: String by project
dependencies {
    compileOnlyApi("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    // Make sure to update net.mcberry.berry.api.ExternalLibrariesInfo too!
    compileOnlyApi("org.ow2.asm:asm:${asm_version}")
    compileOnlyApi("org.ow2.asm:asm-analysis:${asm_version}")
    compileOnlyApi("org.ow2.asm:asm-tree:${asm_version}")
    compileOnlyApi("org.ow2.asm:asm-commons:${asm_version}")
    compileOnlyApi("org.ow2.asm:asm-util:${asm_version}")
    compileOnlyApi("net.fabricmc:sponge-mixin:${mixins_version}")
    compileOnlyApi("io.github.llamalad7:mixinextras-common:${mixinextras_version}")
}

val berry_version: String by project
tasks.jar {
    manifest {
        attributes (
            "Berry-Version" to berry_version,
            "Berry-Base-Mod" to "net.mcberry.berry.api.BuiltinAPIBootstrap",
            "Berry-Base-Mod-Name" to "berrybuiltins"
        )
    }
}

val minecraft_version: String by project
stem {
    minecraftVersion(minecraft_version)

    playerName("BerryDev")

    projectMod {
        archiveTask = "jar"
    }

    transformArgs { jvmArgs, programArgs, classpath, mainClass, runDir ->
        jvmArgs += "-Dberry.indev=true"

        val side: String
        var finalMainClass = mainClass
        if (mainClass == "net.minecraft.bundler.Main") {
            jvmArgs += "-Dberry.side=SERVER"
            jvmArgs += "-DbundlerRepoDir=" + gradle.gradleUserHomeDir.resolve("caches").resolve("stem_mcdev").resolve("server_libraries")
            programArgs.add("--nogui")
            finalMainClass = "net.minecraft.server.Main"
            side = "server"
        } else {
            side = "client"
        }

        classpath += File(runDir, "mods/berry-${berry_version}.jar").absoluteFile
        jvmArgs += "-Dberry.main=$finalMainClass"

        "net.mcberry.berry.loader.BerryLoaderMain"
    }
}

group = "net.mcberry"
version = berry_version
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
