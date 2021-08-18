///////////////////////////////////////////////////////////////////////////////
//  GRADLE CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
plugins {
    java
    id("io.quarkus") version "1.8.1.Final"
}

///////////////////////////////////////////////////////////////////////////////
//  TASKS CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
val runExample by tasks.creating(Jar::class) {
    group = "keyple"
    dependsOn.add("quarkusDev")
}

///////////////////////////////////////////////////////////////////////////////
//  APP CONFIGURATION
///////////////////////////////////////////////////////////////////////////////
val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    /* Keyple dependencies */
    implementation("org.calypsonet.terminal:calypsonet-terminal-reader-java-api:1.0.+") { isChanging = true }
    implementation("org.calypsonet.terminal:calypsonet-terminal-calypso-java-api:1.0.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-common-java-api:2.0.+") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-service-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-distributed-network-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-distributed-local-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-distributed-remote-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-card-calypso-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-plugin-stub-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-plugin-pcsc-java-lib:2.0.0-SNAPSHOT") { isChanging = true }
    implementation("org.eclipse.keyple:keyple-util-java-lib:2.+") { isChanging = true }
    /* Quarkus */
    implementation(enforcedPlatform("io.quarkus:quarkus-universe-bom:1.8.1.Final"))
    //implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-resteasy-jsonb")
    implementation("io.quarkus:quarkus-resteasy")
    implementation("io.quarkus:quarkus-rest-client")
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}
