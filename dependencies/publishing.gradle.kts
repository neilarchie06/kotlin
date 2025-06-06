buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2")
    }
}

apply(plugin = "maven-publish")

val archives by configurations

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            setArtifacts(archives.artifacts)
        }
    }

    repositories {
        maven {
            name = "kotlinSpace"
            url = uri("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
            credentials(org.gradle.api.artifacts.repositories.PasswordCredentials::class)
        }
    }
}