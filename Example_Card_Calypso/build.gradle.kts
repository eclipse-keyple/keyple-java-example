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
    implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.2.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.1.0")
    implementation("org.eclipse.keyple:keyple-service-resource-java-lib:2.0.2")
    implementation("org.eclipse.keyple:keyple-plugin-pcsc-java-lib:2.1.0")
    implementation("org.eclipse.keyple:keyple-plugin-stub-java-lib:2.1.0")
    implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:2.2.1")
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.+") { isChanging = true }
    implementation("org.slf4j:slf4j-simple:1.7.32")
}

val javaSourceLevel: String by project
val javaTargetLevel: String by project
java {
    sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
    targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    println("Compiling Java $sourceCompatibility to Java $targetCompatibility.")
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
    register("fatJarTN313", Jar::class.java) {
        archiveClassifier.set("TN313-fat")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "org.eclipse.keyple.card.calypso.example.UseCase10_SessionTrace_TN313.Main_SessionTrace_TN313_Pcsc")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
    register("fatJarPerfEmbeddedValidation", Jar::class.java) {
        archiveClassifier.set("PerfEmbeddedValidation-fat")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "org.eclipse.keyple.card.calypso.example.UseCase12_PerformanceMeasurement_EmbeddedValidation.Main_PerformanceMeasurement_EmbeddedValidation_Pcsc")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
    register("fatJarPerfDistributedReloading", Jar::class.java) {
        archiveClassifier.set("PerfDistributedReloading-fat")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes("Main-Class" to "org.eclipse.keyple.card.calypso.example.UseCase13_PerformanceMeasurement_DistributedReloading.Main_PerformanceMeasurement_DistributedReloading_Pcsc")
        }
        from(configurations.runtimeClasspath.get()
            .onEach { println("add from dependencies: ${it.name}") }
            .map { if (it.isDirectory) it else zipTree(it) })
        val sourcesMain = sourceSets.main.get()
        sourcesMain.allSource.forEach { println("add from sources: ${it.name}") }
        from(sourcesMain.output)
    }
}