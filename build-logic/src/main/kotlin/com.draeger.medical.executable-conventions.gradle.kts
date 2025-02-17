import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register


plugins {
    id("com.draeger.medical.java-conventions")
    id("com.draeger.medical.kotlin-conventions")
    id("de.undercouch.download")
    id("edu.sc.seis.launch4j")
}

val javaVersion = property("javaVersion").toString()
val jreDirectoryName = "jdk-17.0.5+8-jre"
val jreBasePath = "jre"
val jreFullPath = "${jreBasePath}/${jreDirectoryName}"
val jreDownloadUrlPrefix = "https://github.com/adoptium/temurin17-binaries/releases/download/"
val jreDownloadFileName = "OpenJDK17U-jre_x64_windows_hotspot_17.0.5_8.zip"
val jreDownloadUrlSuffix = "jdk-17.0.5%2B8/${jreDownloadFileName}"
val jreDownloadUrl = "${jreDownloadUrlPrefix}${jreDownloadUrlSuffix}"

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadJre") {
    src(jreDownloadUrl)
    dest(file("${layout.buildDirectory.get().asFile}/${jreDownloadFileName}"))
    overwrite(false)
    onlyIfModified(true)
}

tasks.register<Copy>("unpackJre") {
    doFirst {
        file("${layout.buildDirectory.get().asFile}/").walk().forEach { file ->
            if (!file.setWritable(true)) {
                println("Failed to set writable permission for ${file.absolutePath}")
            }
        }
    }
    dependsOn("downloadJre")
    from(zipTree(file("${layout.buildDirectory.get().asFile}/${jreDownloadFileName}")))
    into("${layout.buildDirectory.get().asFile}/${jreBasePath}")
}

tasks.register("downloadAndUnpackJre") {
    dependsOn("unpackJre")
}

tasks.register<Copy>("copyRuntimeLibs") {
    from(configurations.runtimeClasspath) {
        exclude { it.isDirectory }
    }
    into("${layout.buildDirectory.get().asFile}/lib")
}

tasks.named("createExe") {
    // Use dynamic configuration (note: no compile-time checking)
    (this as ExtensionAware).extensions.extraProperties.apply {
        set("headerType", "console")
        set("jar", "${layout.buildDirectory.get().asFile}/libs/${project.name}-${project.version}.jar")
        set("outfile", "${project.name}-${project.version}.exe")
        set("mainClassName", findProperty("mainClass")?.toString() ?: "com.draeger.medical.sdccc.TestSuite")
        set("classpath", setOf("lib/**"))
        set("jreMinVersion", javaVersion)
        set("bundledJrePath", "./${jreFullPath}")
        set("version", "${project.version}.0")
        set("textVersion", "${project.version}")
        set("fileDescription", "${project.name}")
        set("copyright", "2023-2024 Draegerwerk AG & Co. KGaA")
        set("productName", "${project.name}")
        set("companyName", "Draegerwerk AG & Co. KGaA")
        set("internalName", "sdccc")
    }
}

