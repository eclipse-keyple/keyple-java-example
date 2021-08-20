pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        google()
        jcenter()
    }
}

rootProject.name = "keyple-java-example"
//include("sources:card-calypso")
//include("sources:card-generic")
//include("sources:plugin-pcsc")
include("sources:plugin-android-nfc")
//include("sources:plugin-android-omapi")
//include("sources:service-resource")



// Fix resolution of dependencies with dynamic version in order to use SNAPSHOT first when available.
// See explanation here : https://docs.gradle.org/6.8.3/userguide/single_versions.html
enableFeaturePreview("VERSION_ORDERING_V2")