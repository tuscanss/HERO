pluginManagement {
    repositories {
        google()
        // Oglinda Google a Maven Central (mai stabilă decât repo.maven.apache.org)
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // Oglinda Google a Maven Central (mai stabilă decât repo.maven.apache.org)
        maven("https://maven-central.storage-download.googleapis.com/maven2/")
        mavenCentral()
        maven("https://jitpack.io")
    }
}
rootProject.name = "IrRemote"
include(":app")
