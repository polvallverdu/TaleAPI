plugins {
    id("java")
    id("maven-publish")
}

group = "dev.polv.taleapi"
// Version from environment (CI sets this from git tag), fallback to SNAPSHOT for local dev
version = System.getenv("VERSION") ?: "0.1.0-SNAPSHOT"

// Project metadata
val authorName = "Pol Vallverdu"
val authorWebsite = "https://polv.dev"
val projectUrl = "https://github.com/polvallverdu/TaleAPI"
val projectDescription = "A library for creating Hytale mods"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Disable annotation processing when compiling TaleAPI itself
// (the processor can't process itself during its own compilation)
tasks.compileJava {
    options.compilerArgs.add("-proc:none")
}

// JAR Manifest attributes
tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to authorName,
            "Implementation-Vendor-Id" to project.group,
            "Implementation-URL" to projectUrl,
            "Specification-Title" to project.name,
            "Specification-Version" to project.version,
            "Specification-Vendor" to authorName,
            "Built-By" to authorName,
            "Bundle-License" to "MIT",
            "Bundle-Vendor" to authorName,
            "Bundle-DocURL" to authorWebsite,
            "Automatic-Module-Name" to "dev.polv.taleapi"
        )
    }
    // Include LICENSE in the JAR
    from("LICENSE") {
        into("META-INF")
    }
}

tasks.javadoc {
    options {
        this as StandardJavadocDocletOptions
        encoding = "UTF-8"
        docTitle = "TaleAPI $version"
        windowTitle = "TaleAPI $version"
        links("https://docs.oracle.com/en/java/javase/17/docs/api/")
        addBooleanOption("html5", true)
        // Author and version tags in Javadoc
        author(true)
        version(true)
    }
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            
            pom {
                name.set("TaleAPI")
                description.set(projectDescription)
                url.set(projectUrl)
                inceptionYear.set("2025")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                        distribution.set("repo")
                    }
                }
                
                developers {
                    developer {
                        id.set("polvallverdu")
                        name.set(authorName)
                        url.set(authorWebsite)
                    }
                }
                
                scm {
                    url.set(projectUrl)
                    connection.set("scm:git:git://github.com/polvallverdu/TaleAPI.git")
                    developerConnection.set("scm:git:ssh://git@github.com/polvallverdu/TaleAPI.git")
                }
                
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("$projectUrl/issues")
                }
            }
        }
    }
    
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/polvallverdu/TaleAPI")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}