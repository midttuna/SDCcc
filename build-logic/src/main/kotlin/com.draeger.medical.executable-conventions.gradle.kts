import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.named
import org.gradle.api.provider.Provider
import edu.sc.seis.launch4j.tasks.DefaultLaunch4jTask

plugins {
    id("com.draeger.medical.java-conventions")
    id("com.draeger.medical.kotlin-conventions")
    id("de.undercouch.download")
    id("edu.sc.seis.launch4j")
}

val javaVersion: String by project


val jreDirectoryName = "jdk-17.0.5+8-jre"
val jreBasePath = "jre"
val jreFullPath = "$jreBasePath/$jreDirectoryName"
val jreDownloadUrlPrefix = "https://github.com/adoptium/temurin17-binaries/releases/download/"
val jreDownloadFileName = "OpenJDK17U-jre_x64_windows_hotspot_17.0.5_8.zip"
val jreDownloadUrlSuffix = "jdk-17.0.5%2B8/$jreDownloadFileName"
val jreDownloadUrl = "$jreDownloadUrlPrefix$jreDownloadUrlSuffix"


val buildDirProvider = layout.buildDirectory.map { it.asFile }

tasks.register<de.undercouch.gradle.tasks.download.Download>("downloadJre") {
    src(jreDownloadUrl)
    dest(provider { buildDirProvider.get().resolve(jreDownloadFileName) })
    overwrite(false)
    onlyIfModified(true)
}

tasks.register<Copy>("unpackJre") {
    doFirst {
        buildDirProvider.get().walk().forEach { file ->
            if (!file.setWritable(true)) {
                println("Failed to set writable permission for ${file.absolutePath}")
            }
        }
    }
    dependsOn(tasks.named("downloadJre"))
    from(provider { zipTree(buildDirProvider.get().resolve(jreDownloadFileName)) })
    into(provider { buildDirProvider.get().resolve(jreBasePath) })
}

tasks.register("downloadAndUnpackJre") {
    dependsOn(tasks.named("unpackJre"))
}

tasks.register<Copy>("copyRuntimeLibs") {
    from(configurations.runtimeClasspath) {
        exclude { it.isDirectory }
    }
    into(provider { buildDirProvider.get().resolve("lib") })
}


tasks.named("createExe", DefaultLaunch4jTask::class) {
    dependsOn(tasks.named("copyRuntimeLibs"), tasks.named("downloadAndUnpackJre"))

    headerType = "console"
    jar = provider {

        layout.buildDirectory.get().asFile.resolve("libs/${project.name}-${project.version}.jar").absolutePath
    }.get()
    outfile = "${project.name}-${project.version}.exe" // Relative path: file will be placed in build/launch4j.
    mainClassName = findProperty("mainClass")?.toString() ?: "com.draeger.medical.sdccc.TestSuite"
    classpath = setOf("lib/**")
    jreMinVersion = javaVersion
    bundledJrePath = "./$jreFullPath"

    version = "${project.version}.0"
    textVersion = "${project.version}"
    fileDescription = "${project.name}"
    copyright = "2023-2024 Draegerwerk AG & Co. KGaA"

    productName = "${project.name}"
    companyName = "Draegerwerk AG & Co. KGaA"
    internalName = "sdccc"
}

tasks.named("build") {
    dependsOn(tasks.named("createExe"))
}
dependencies {
    api(libs.gradleplugins.launch4j)
}