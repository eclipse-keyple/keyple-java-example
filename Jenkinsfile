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
        sh 'cd ./sources/Example_Card_Calypso && ./gradlew clean build --no-build-cache --info --stacktrace'
      } }
    }
    stage('Build Example Card Generic') {
      steps { container('java-builder') {
        sh 'cd ./sources/Example_Card_Generic && ./gradlew clean build --no-build-cache --info --stacktrace'
      } }
    }
    stage('Build Example Service Resource') {
      steps { container('java-builder') {
        sh 'cd ./sources/Example_Service_Resource && ./gradlew clean build --no-build-cache --info --stacktrace'
      } }
    }
    stage('Build Example Plugin PC/SC') {
      steps { container('java-builder') {
        sh 'cd ./sources/Example_Plugin_PCSC && ./gradlew clean build --no-build-cache --info --stacktrace'
      } }
    }
    stage('Build Example Plugin Android NFC') {
      steps { container('java-builder') {
        sh 'cd ./sources/Example_Plugin_Android_NFC && ./gradlew clean build --no-build-cache --info --stacktrace'
      } }
    }
    stage('Build Example Plugin Android OMAPI') {
      steps { container('java-builder') {
        sh 'cd ./sources/Example_Plugin_Android_OMAPI && ./gradlew clean build --no-build-cache --info --stacktrace'
      } }
    }
  }
}
