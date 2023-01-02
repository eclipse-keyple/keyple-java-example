///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    java
    id("com.diffplug.spotless") version "5.10.2"
    id("io.quarkus") version "1.8.1.Final"
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

val javaSourceLevel: String by project
val javaTargetLevel: String by project
java {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    println("Compiling Java $sourceCompatibility to Java $targetCompatibility.")
}

dependencies {
    /* Keyple dependencies */
    implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.1.+") { isChanging = true }
    implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.4.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.1.1")
    implementation("org.eclipse.keyple:keyple-distributed-network-java-lib:2.0.0")
    implementation("org.eclipse.keyple:keyple-distributed-local-java-lib:2.0.0")
    implementation("org.eclipse.keyple:keyple-distributed-remote-java-lib:2.0.0")
    implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:2.3.1")
    implementation("org.eclipse.keyple:keyple-plugin-stub-java-lib:2.1.0")
    implementation("org.eclipse.keyple:keyple-plugin-pcsc-java-lib:2.1.0")
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.+") { isChanging = true }
    /* Quarkus */
    implementation(enforcedPlatform("io.quarkus:quarkus-universe-bom:1.8.1.Final"))
    implementation("io.quarkus:quarkus-resteasy-jsonb")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("io.quarkus:quarkus-rest-client")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
tasks {
    spotless {
        java {
            target("**/src/**/*.java")
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
            importOrder("java", "javax", "org", "com", "")
            removeUnusedImports()
            googleJavaFormat()
        }
    }
}
val runExample by tasks.creating(Jar::class) {
    group = "keyple"
    dependsOn.add("quarkusDev")
}
