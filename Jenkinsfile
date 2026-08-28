pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                git branch: 'developer', url: 'https://github.com/giouhciha/octomind-booksreader.git', credentialsId: ''
            }
        }

        stage('Permisos Gradle') {
            steps {
                sh 'chmod +x gradlew'
            }
        }

        stage('Lint (Android)') {
            steps {
                sh './gradlew lint'
            }
        }

        stage('Ktlint / Detekt') {
            steps {
                sh './gradlew ktlintCheck detekt'
            }
        }

        stage('Unit Tests') {
            steps {
                sh './gradlew testDebugUnitTest'
            }
        }
    }

    post {
        always {
            recordIssues(tools: [androidLintParser(pattern: '**/build/reports/lint-results-*.xml')])
            junit '**/build/test-results/**/*.xml'
        }
    }
}
