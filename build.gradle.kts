import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("idea")
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
    id("pmd")
    id("java-library")
    id("io.typecraft.gradlesource.spigot") version "1.0.0"
}

group = "com.ravingarinc.portals"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()

    maven("https://mvn.lumine.io/repository/maven-public/") {
        content {
            includeGroup("io.lumine")
        }
    }

    maven("https://nexus.phoenixdevt.fr/repository/maven-public/") {
        content {
            includeGroup("net.Indyuce")
            includeGroup("io.lumine")
        }
    }

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/groups/public/")


    maven("https://maven.enginehub.org/repo/")

    maven("https://jitpack.io")
}

dependencies {
    library(kotlin("stdlib"))
    library("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.0-Beta")

    library("com.github.shynixn.mccoroutine", "mccoroutine-bukkit-api", "2.11.0")
    library("com.github.shynixn.mccoroutine", "mccoroutine-bukkit-core", "2.11.0")

    implementation("com.ravingarinc.api:common:1.3.1")
    implementation("com.ravingarinc.api:module:1.3.1")
    implementation("org.jetbrains:annotations:23.1.0")

    //compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")

    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("io.lumine:Mythic-Dist:5.1.0-SNAPSHOT")
    compileOnly("io.lumine:MythicLib-dist:1.5.2-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.9.2-SNAPSHOT")

    testImplementation("org.jetbrains:annotations:23.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks {
    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    shadowJar {
        archiveBaseName.set("PortalsPlus")
        archiveClassifier.set("")
        archiveVersion.set("")
        relocate("com.ravingarinc.api", "com.ravingarinc.portals.libs.api")
    }

    artifacts {
        archives(shadowJar)
    }

    register<Copy>("copyToDev") {
        from(shadowJar)
        into(project.layout.projectDirectory.dir("../../Desktop/Programming/Server/plugins"))
        //into "E:/Documents/Workspace/Servers/1.18.2-TEST/plugins/"
    }

    assemble {
        dependsOn(shadowJar)
        finalizedBy("copyToDev")
    }
    test {
        useJUnitPlatform()
        // Ensure testing is never "up-to-date" (in Gradle-speak), which means it can never be skipped,
        // as it would otherwise be.
        outputs.upToDateWhen { false }

        // Ensure we get all the useful test output.
        testLogging {
            events("failed", "passed", "skipped")
            showExceptions = true
            showCauses = true
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

bukkit {

    name = "PortalsPlus"
    version = project.version as String
    description = "Controllable portal settings!"
    main = "com.ravingarinc.portals.PortalsPlus"

    // API version (should be set for 1.13+)
    apiVersion = "1.18"

    // Other possible properties from plugin.yml (optional)
    author = "RAVINGAR"
    softDepend = listOf("MMOItems", "MythicMobs", "MythicLib")

    commands {
        register("portals") {
            aliases = listOf("p", "portal")
            description = "PortalsPlus Admin Command"
            usage = "Unknown argument. Try /portals ?"
        }
    }
}
