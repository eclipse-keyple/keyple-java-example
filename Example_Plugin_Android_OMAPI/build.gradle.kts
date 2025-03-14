///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    id("com.diffplug.spotless") version "6.25.0"
    id("org.sonarqube") version "3.1"
    id("org.jetbrains.dokka") version "1.7.10"
}

buildscript {
    val kotlinVersion: String by project
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("com.android.tools.build:gradle:7.4.2")
        classpath ("javax.xml.bind:jaxb-api:2.3.1")
        classpath ("com.sun.xml.bind:jaxb-impl:2.3.9")
        classpath("org.eclipse.keyple:keyple-gradle:0.2.+") { isChanging = true }
    }
}