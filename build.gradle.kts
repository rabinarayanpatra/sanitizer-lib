plugins {
    java
    jacoco
    `maven-publish`
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.4.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("net.ltgt.errorprone") version "5.1.0" apply false
}

allprojects {
    group = "io.github.rabinarayanpatra.sanitizer"
    version = "1.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.vanniktech.maven.publish")
    apply(plugin = "jacoco")
    apply(plugin = "net.ltgt.errorprone")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:4.0.6")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.named("jacocoTestReport"))
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    dependencies {
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        "errorprone"("com.google.errorprone:error_prone_core:2.49.0")
        "api"("org.jspecify:jspecify:1.0.0")
    }

    mavenPublishing {
        publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
        signAllPublications()
        
        coordinates(group.toString(), project.name, version.toString())

        pom {
            name.set(project.name)
            description.set("Sanitizer Library")
            url.set("https://github.com/rabinarayanpatra/sanitizer-lib")
            licenses {
                license {
                    name.set("MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("rabinarayanpatra")
                    name.set("Rabinarayan Patra")
                    email.set("rabinarayanpatra1999@gmail.com")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/rabinarayanpatra/sanitizer-lib.git")
                developerConnection.set("scm:git:ssh://github.com/rabinarayanpatra/sanitizer-lib.git")
                url.set("https://github.com/rabinarayanpatra/sanitizer-lib")
            }
        }
    }

    apply(plugin = "com.diffplug.spotless")
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            eclipse()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

// Aggregated JaCoCo coverage report across all subprojects.
// Run with: ./gradlew test jacocoAggregatedReport
tasks.register<JacocoReport>("jacocoAggregatedReport") {
    group = "verification"
    description = "Aggregates JaCoCo coverage reports from all subprojects."

    dependsOn(subprojects.map { it.tasks.withType<Test>() })

    val reports = subprojects.flatMap { sub ->
        sub.tasks.withType<JacocoReport>()
    }

    executionData.setFrom(reports.flatMap { it.executionData.files })
    sourceDirectories.setFrom(reports.flatMap { it.sourceDirectories.files })
    classDirectories.setFrom(reports.flatMap { it.classDirectories.files })

    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregated/jacoco.xml"))
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregated/html"))
    }
}
