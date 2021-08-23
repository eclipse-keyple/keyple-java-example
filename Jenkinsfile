#!groovy
pipeline {
  environment {
    PROJECT_NAME = "keyple-java-example"
    PROJECT_BOT_NAME = "Eclipse Keyple Bot"
  }
  agent { kubernetes { yaml javaBuilder('2.0') } }
  stages {
    stage('Build Plugin Android NFC Example') {
      steps { container('java-builder') {
        sh 'cd ./sources/Example_Plugin_Android_NFC && ./gradlew clean build --no-build-cache --info --stacktrace'
      } }
    }
  }
}
