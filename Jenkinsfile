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
                // En Windows no hace falta chmod, se omite este stage
                echo 'Windows agent - chmod no aplica'
            }
        }

        stage('Lint (Android)') {
            steps {
                bat 'gradlew.bat lint'
            }
        }

        stage('Ktlint / Detekt') {
            steps {
                bat 'gradlew.bat ktlintCheck detekt'
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
            recordIssues(tools: [androidLintParser(pattern: '**/build/reports/lint-results-*.xml')])
            junit '**/build/test-results/**/*.xml'
        }
    }
}
