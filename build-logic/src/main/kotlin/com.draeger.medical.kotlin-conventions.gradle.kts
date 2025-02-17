import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.register

plugins {
    id("com.draeger.medical.java-conventions")
    id("org.jetbrains.kotlin.jvm")
}


val javaVersion: String by project

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = javaVersion
    }
}

val detekt by configurations.creating


val detektConfigPath = providers.provider {
    projectDir.resolve(project.findProperty("detektConfigFilePath")?.toString() ?: "dev_config/detekt.yml")
}

// container to lazily call asPath upon evaluation, not during configuration, which can be too early
data class LazyToStringFileCollection(private val fileCollection: FileCollection) {
    override fun toString(): String = fileCollection.asPath
}

tasks.register<JavaExec>("detekt") {
    dependsOn(tasks.named("assemble"))
    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
    classpath = detekt

    val input = projectDir
    // The config is obtained via the provider when the task executes
    val config = detektConfigPath.get().absolutePath
    val exclude = ".*/build/.*,.*/resources/.*,**/build.gradle.kts,**/settings.gradle.kts"
    val classpathNeededForDetekt = LazyToStringFileCollection(
        project.sourceSets["main"].runtimeClasspath +
            project.sourceSets["test"].runtimeClasspath +
            project.sourceSets["main"].compileClasspath +
            project.sourceSets["test"].compileClasspath
    )

    val jdkHome = System.getProperty("java.home")
    args(
        "--input", input.absolutePath,
        "--config", config,
        "--excludes", exclude,
        "--report", provider { "html:${layout.buildDirectory.get().asFile.resolve("reports/detekt/detekt.html")}" }.get(),
        "--classpath", classpathNeededForDetekt,
        "--jdk-home", jdkHome,
        "--jvm-target", javaVersion,
        "--build-upon-default-config"
    )
}

dependencies {
    detekt(libs.detekt.cli)
    detekt(libs.detekt.formatting)
    api(libs.org.jetbrains.kotlin.kotlin.stdlib)
}
