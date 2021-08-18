include(":sources:solution-distributed:UseCase1_ReaderClientSide_Webservice")
//include(":sources:solution-distributed:UseCase1_ReaderClientSide_Websocket")
//include(":sources:solution-distributed:UseCase7_PoolReaderServerSide_Webservice")

rootProject.name = "keyple-java-example"

// Fix resolution of dependencies with dynamic version in order to use SNAPSHOT first when available.
// See explanation here : https://docs.gradle.org/6.8.3/userguide/single_versions.html
enableFeaturePreview("VERSION_ORDERING_V2")