plugins {
    `java-library`
}

group = "com.hibernet"
version = "0.2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "mclhr",
            "Implementation-Version" to project.version,
            "Main-Class" to "com.hibernet.mclhr.MclhrCli",
        )
    }
}
