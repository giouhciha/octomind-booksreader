pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'developer', url: 'https://github.com/giouhciha/octomind-booksreader.git', credentialsId: ''
            }
        }

        stage('Lint (Android)') {
            steps {
                bat 'gradlew.bat lintDebug'
            }
        }

        stage('Unit Tests') {
            steps {
                bat 'gradlew.bat testDebugUnitTest'
            }
        }
    }

    post {
        always {
            recordIssues(
                enabledForFailure: true,
                tools: [androidLintParser(pattern: 'app/build/reports/lint-results-debug.xml')]
            )
            junit(
                testResults: 'app/build/test-results/testDebugUnitTest/TEST-*.xml',
                allowEmptyResults: true
            )
        }
    }
}
