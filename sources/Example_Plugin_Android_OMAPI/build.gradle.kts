///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    id("com.diffplug.spotless") version "5.10.2"
    id("com.android.application") version "4.2.2"
    kotlin("android") version "1.4.20"
    kotlin("android.extensions") version "1.4.20"
}
buildscript {
    val kotlinVersion: String by project
    repositories {
        mavenLocal()
        maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
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
group = "org.eclipse.keyple"
repositories {
    mavenLocal()
    maven(url = "https://repo.eclipse.org/service/local/repositories/maven_central/content")
    mavenCentral()
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
    maven(url = "https://s01.oss.sonatype.org/content/repositories/snapshots")
    google()
    jcenter()
}

val kotlinVersion: String by project
val archivesBaseName: String by project
android {
    compileSdkVersion(29)
    buildToolsVersion("30.0.2")

    defaultConfig {
        applicationId("org.eclipse.keyple.plugin.android.omapi.example")
        minSdkVersion(19)
        targetSdkVersion(29)
        versionName(project.version.toString())

        testInstrumentationRunner("android.support.test.runner.AndroidJUnitRunner")
        multiDexEnabled = true
    }

    buildTypes {
        getByName("release") {
            minifyEnabled(false)
            isTestCoverageEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    val javaSourceLevel: String by project
    val javaTargetLevel: String by project
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(javaSourceLevel)
        targetCompatibility = JavaVersion.toVersion(javaTargetLevel)
    }

    packagingOptions {
        exclude("META-INF/NOTICE.md")
    }

    kotlinOptions {
        jvmTarget = javaTargetLevel
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
    }
}

dependencies {

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"), "exclude" to listOf("org.simalliance.openmobileapi.jar"))))

    //Keyple Common
    implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-card-generic-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-plugin-android-omapi-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.0.0-SNAPSHOT") { isChanging = true }

    /*
    Android components
    */
    implementation("androidx.appcompat:appcompat:1.2.0")
    implementation("com.google.android.material:material:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("androidx.activity:activity-ktx:1.2.1")
    implementation("androidx.fragment:fragment-ktx:1.3.1")

    /*
    Log
    */
    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("com.arcao:slf4j-timber:3.1@aar") //SLF4J binding for Timber

    /*
    Kotlin
    */
    implementation("androidx.core:core-ktx:1.3.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    /*
    Coroutines
    */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.3.3")

    implementation("androidx.multidex:multidex:2.0.1")

    testImplementation("junit:junit:4.12")
    androidTestImplementation("com.android.support.test:runner:1.0.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
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
            target("src/**/*.java")
            licenseHeaderFile("${project.rootDir}/LICENSE_HEADER")
            importOrder("java", "javax", "org", "com", "")
            removeUnusedImports()
            googleJavaFormat()
        }
    }
}
