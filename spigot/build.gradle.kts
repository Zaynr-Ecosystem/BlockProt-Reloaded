buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
}

plugins {
    id("maven-publish")
    // Updated to com.gradleup.shadow which supports Java 25+ (ASM updated)
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
}

val nbtApiVersion = project.property("nbtApiVersion") as String
val townyVersion = project.property("townyVersion") as String
val papiVersion = project.property("papiVersion") as String
val worldGuardVersion = project.property("worldGuardVersion") as String

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
    }
    maven("https://oss.sonatype.org/content/repositories/snapshots/") {
        name = "Sonatype Snapshots"
    }
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.codemc.org/repository/maven-public/") {
        name = "CodeMC"
        content {
            includeGroup("de.tr7zw")
        }
    }
    maven("https://repo.tcoded.com/releases") {
        name = "TCoded"
    }
    maven("https://repo.opencollab.dev/main/") {
        name = "OpenCollab"
    }
}

dependencies {
    implementation(project(":common"))

    // Compiled against the oldest supported version so newer-only APIs cannot slip in.
    compileOnly("io.papermc.paper:paper-api:${project.findProperty("paperApiVersion") ?: "1.21.7-R0.1-SNAPSHOT"}")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.apache.commons:commons-lang3:3.21.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.3.0")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.116.3")

    // bStats: 3.2.1
    api("org.bstats:bstats-bukkit:3.2.1")

    // Dependencies
    implementation("de.tr7zw:item-nbt-api:$nbtApiVersion")

    // FoliaLib: cross-platform scheduler (Spigot / Paper / Purpur / Pufferfish / Folia)
    implementation("com.tcoded:FoliaLib:0.5.1")

    implementation("org.enginehub:squirrelid:0.3.2")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("com.mysql:mysql-connector-j:26.7.0")

    // Adventure: bundled for servers that do not expose Adventure to the plugin
    // classpath (vanilla Spigot/Bukkit). Paper-based servers resolve net.kyori.*
    // parent-first, so the bundled copies are only used on servers without it.
    implementation("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("net.kyori:adventure-text-serializer-legacy:4.17.0")
    implementation("net.kyori:adventure-text-serializer-plain:4.17.0")

    // Integrations (soft-depend, provided at runtime by the server)
    compileOnly("com.github.TownyAdvanced:Towny:$townyVersion")
    compileOnly("me.clip:placeholderapi:$papiVersion")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:$worldGuardVersion")
    compileOnly("com.github.angeschossen:LandsAPI:6.28.11")
    compileOnly("com.cjburkey.claimchunk:claimchunk:0.0.25-FIX3")
    compileOnly("com.github.Zrips:Residence:6.0.2.3") { isTransitive = false }
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.7") { isTransitive = false }
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly("org.geysermc.cumulus:cumulus:1.1.2")
}

val targetJavaVersion = project.property("targetJavaVersion") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withJavadocJar()
    withSourcesJar()
}

val pluginVersion: String = project.version.toString()

tasks.processResources {
    inputs.property("version", pluginVersion)

    filesMatching(listOf("plugin.yml")) {
        expand("version" to pluginVersion)
    }
}

tasks.javadoc {
    options {
            source = "21"
        encoding = "UTF-8"
        memberLevel = JavadocMemberLevel.PACKAGE
        (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
    }

    this.isFailOnError = false
}

tasks.shadowJar {
    relocate("de.tr7zw.changeme.nbtapi", "de.sean.blockprot.bukkit.shaded.nbtapi")
    relocate("org.bstats", "de.sean.blockprot.bukkit.metrics")
    relocate("org.enginehub.squirrelid", "de.sean.blockprot.bukkit.squirrelid")
    relocate("com.zaxxer.hikari", "de.sean.blockprot.bukkit.shaded.hikari")
    relocate("com.tcoded.folialib", "de.sean.blockprot.bukkit.shaded.folialib")
    relocate("com.github.benmanes.caffeine", "de.sean.blockprot.bukkit.shaded.caffeine")
    // minimize()

    dependencies {
        this.include(project(":common"))
        this.include(dependency("org.jetbrains:annotations"))
        this.include(dependency("de.tr7zw:item-nbt-api"))
        this.include(dependency("org.bstats:bstats-base"))
        this.include(dependency("org.bstats:bstats-bukkit"))
        this.include(dependency("com.tcoded:FoliaLib"))
        this.include(dependency("org.enginehub:squirrelid"))
        this.include(dependency("com.zaxxer:HikariCP"))
        this.include(dependency("com.mysql:mysql-connector-j"))
        this.include(dependency("org.slf4j:slf4j-api"))
        this.include(dependency("com.github.ben-manes.caffeine:caffeine"))
        this.include(dependency("net.kyori:adventure-api"))
        this.include(dependency("net.kyori:adventure-key"))
        this.include(dependency("net.kyori:adventure-text-minimessage"))
        this.include(dependency("net.kyori:adventure-text-serializer-legacy"))
        this.include(dependency("net.kyori:adventure-text-serializer-plain"))
        this.include(dependency("net.kyori:option"))
        this.include(dependency("net.kyori:examination-api"))
        this.include(dependency("net.kyori:examination-string"))
    }

    archiveFileName.set("${project.property("jarBaseName")}-${project.version}.jar")
    exclude("META-INF/*.kotlin_module")
}

tasks.build {
    dependsOn(tasks["javadocJar"])
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}

tasks.runServer {
    val mcVer = project.findProperty("mcVersion") as? String ?: "26.3"
    minecraftVersion(mcVer)
    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
        "-Dterminal.ansi=true"
    )
    downloadPlugins {
        url("https://download.luckperms.net/1561/bukkit/loader/LuckPerms-Bukkit-5.5.71.jar")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String

            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}

val supportedMinecraftVersions: List<String> = rootProject.file("gradle.properties").readLines()
    .first { it.startsWith("# Supported on blockProtVersion") }
    .substringAfter(":")
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }

hangarPublish {
    publications.register("plugin") {
        version.set(project.version.toString())
        id.set("BlockProt-Reloaded")
        channel.set("Release")
        apiKey.set(providers.environmentVariable("HANGAR_API_TOKEN"))
        val stagedNotes = rootProject.file("build/reports/release-notes.md")
        val notesFile = rootProject.file("docs/RELEASE_NOTES/${project.version}.RELEASE_NOTES.md")
        if (stagedNotes.exists()) {
            changelog.set(stagedNotes.readText(Charsets.UTF_8))
        } else if (notesFile.exists()) {
            changelog.set(notesFile.readText(Charsets.UTF_8))
        }
        platforms {
            paper {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(supportedMinecraftVersions)
            }
        }
    }
}
