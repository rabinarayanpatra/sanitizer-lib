plugins {
    java
    `maven-publish`
    id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.rabinarayanpatra.sanitizer"
version = "1.0.21"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        withSourcesJar()
        withJavadocJar()
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
    
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
                
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
        }
        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrhUsername") as String?
                    password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrhPassword") as String?
                }
            }
        }
    }

    configure<SigningExtension> {
        useGpgCmd()
        sign(publishing.publications["maven"])
    }
}
