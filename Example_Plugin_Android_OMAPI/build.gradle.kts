///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    id("com.diffplug.spotless") version "5.10.2"
    id("org.sonarqube") version "3.1"
    id("org.jetbrains.dokka") version "1.4.32"
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
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath("org.eclipse.keyple:keyple-gradle:0.2.+") { isChanging = true }
    }
}
apply(plugin = "org.eclipse.keyple")

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
allprojects {
    group = "org.eclipse.keyple"
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
        google()
    }
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        kotlin {
            target("**/*.kt")
            ktlint()
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
        }
        java {
            target("app/src/**/*.java")
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
            importOrder("java", "javax", "org", "com", "")
            removeUnusedImports()
            googleJavaFormat()
        }
    }
    sonarqube {
        properties {
            property("sonar.projectKey", "eclipse_" + project.name)
            property("sonar.organization", "eclipse")
            property("sonar.host.url", "https://sonarcloud.io")
            property("sonar.login", System.getenv("SONAR_LOGIN"))
            System.getenv("BRANCH_NAME")?.let {
                if (it != "main") {
                    property("sonar.branch.name", it)
                }
            }
        }
    }
}
