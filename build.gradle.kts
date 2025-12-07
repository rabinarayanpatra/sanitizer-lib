plugins {
    java
    `maven-publish`
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "6.25.0"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

allprojects {
    group = "io.github.rabinarayanpatra.sanitizer"
    version = "1.0.21"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "com.vanniktech.maven.publish")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.4.5")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
