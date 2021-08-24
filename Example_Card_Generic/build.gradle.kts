///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    java
    id("com.diffplug.spotless") version "5.10.2"
}
buildscript {
    repositories {
        mavenLocal()
        maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
        mavenCentral()
    }
    dependencies {
        classpath("org.eclipse.keyple:keyple-gradle:0.2.+") { isChanging = true }
    }
}
apply(plugin = "org.eclipse.keyple")

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
repositories {
    mavenLocal()
    maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
}
dependencies {
    implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.0.+") { isChanging = true }
    implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.0.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-card-generic-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-plugin-pcsc-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.+") { isChanging = true }
    implementation ("org.slf4j:slf4j-simple:1.7.25")
    implementation ("org.slf4j:slf4j-ext:1.7.25")
}

val javaSourceLevel: String by project
val javaTargetLevel: String by project
java {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    println("Compiling Java $sourceCompatibility to Java $targetCompatibility.")
    withJavadocJar()
    withSourcesJar()
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        java {
            target("src/**/*.java")
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
            importOrder("java", "javax", "org", "com", "")
            removeUnusedImports()
            googleJavaFormat()
        }
    }
}
