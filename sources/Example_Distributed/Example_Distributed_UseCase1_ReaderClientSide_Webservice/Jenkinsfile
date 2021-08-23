#!groovy
pipeline {
  environment {
    PROJECT_NAME = "keyple-integration-java-test"
    PROJECT_BOT_NAME = "Eclipse Keyple Bot"
  }
  agent { kubernetes { yaml javaBuilder('2.0') } }
  stages {
    stage('Prepare settings') { steps { container('java-builder') {
      script {
        env.KEYPLE_VERSION = sh(script: 'grep version gradle.properties | cut -d= -f2 | tr -d "[:space:]"', returnStdout: true).trim()
        echo "Building version ${env.KEYPLE_VERSION} in branch ${env.GIT_BRANCH}"
      }
    } } }
    stage('Build and Test') {
      steps { container('java-builder') {
        sh './gradlew clean test --no-build-cache --info --stacktrace'
        junit testResults: 'build/test-results/test/*.xml', allowEmptyResults: true
      } }
    }
  }
  post { always { container('java-builder') {
    archiveArtifacts artifacts: 'build*/libs/**', allowEmptyArchive: true
    archiveArtifacts artifacts: 'build*/reports/tests/**', allowEmptyArchive: true
    archiveArtifacts artifacts: 'build*/reports/jacoco/test/html/**', allowEmptyArchive: true
  } } }
}
