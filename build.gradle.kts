plugins {
    kotlin("jvm") version "2.0.21"
    application
}
repositories { mavenCentral() }
dependencies { testImplementation(kotlin("test")) }
kotlin { jvmToolchain(17) }
application { mainClass.set("desktop.MainKt") }
sourceSets { main { resources.srcDirs("assets") } }

// fat-jar: все зависимости + assets + main-класс
tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes["Main-Class"] = "desktop.MainKt" }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}
