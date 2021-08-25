#!groovy
pipeline {
  environment {
    PROJECT_NAME = "keyple-java-example"
    PROJECT_BOT_NAME = "Eclipse Keyple Bot"
  }
  agent { kubernetes { yaml javaBuilder('2.0') } }
  stages {
    stage('Prepare settings') { steps { container('java-builder') {
      script {
        sh 'git lfs fetch && git lfs checkout'
      }
    } } }
    stage('Build Example Card Calypso') {
      steps { container('java-builder') {
        sh 'cd ./Example_Card_Calypso && ./gradlew clean spotlessCheck classes --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Card Generic') {
      steps { container('java-builder') {
        sh 'cd ./Example_Card_Generic && ./gradlew clean spotlessCheck classes --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Service Resource') {
      steps { container('java-builder') {
        sh 'cd ./Example_Service_Resource && ./gradlew clean spotlessCheck classes --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Plugin PC/SC') {
      steps { container('java-builder') {
        sh 'cd ./Example_Plugin_PCSC && ./gradlew clean spotlessCheck classes --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Plugin Android NFC') {
      steps { container('java-builder') {
        sh 'cd ./Example_Plugin_Android_NFC && ./gradlew clean build --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Plugin Android OMAPI') {
      steps { container('java-builder') {
        sh 'cd ./Example_Plugin_Android_OMAPI && ./gradlew clean build --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Distributed PoolReaderServerSide Webservice') {
      steps { container('java-builder') {
        sh 'cd ./Example_Distributed_PoolReaderServerSide_Webservice && ./gradlew clean spotlessCheck classes --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Distributed ReaderClientSide Webservice') {
      steps { container('java-builder') {
        sh 'cd ./Example_Distributed_ReaderClientSide_Webservice && ./gradlew clean spotlessCheck classes --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
    stage('Build Example Distributed ReaderClientSide Websocket') {
      steps { container('java-builder') {
        sh 'cd ./Example_Distributed_ReaderClientSide_Websocket && ./gradlew clean spotlessCheck classes --no-build-cache --no-daemon --info --stacktrace'
      } }
    }
  }
}
