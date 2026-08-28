pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Android\\Android Studio\\jbr'
        ANDROID_HOME = 'C:\\Users\\gio_u\\AppData\\Local\\Android\\Sdk'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'developer', url: 'https://github.com/giouhciha/octomind-booksreader.git', credentialsId: ''
            }
        }

        stage('Lint (Android)') {
            steps {
                bat '"%JAVA_HOME%\\bin\\java.exe" -version'
                bat 'gradlew.bat --version'
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
            script {
                if (fileExists('app/build/reports/lint-results-debug.xml')) {
                    recordIssues(
                        enabledForFailure: true,
                        tools: [androidLintParser(pattern: 'app/build/reports/lint-results-debug.xml')]
                    )
                } else {
                    echo 'Android Lint no genero un reporte; revisa el error de la etapa Lint (Android).'
                }

                if (fileExists('app/build/test-results/testDebugUnitTest')) {
                    junit testResults: 'app/build/test-results/testDebugUnitTest/TEST-*.xml'
                } else {
                    echo 'No hay resultados unitarios porque la etapa no se ejecuto o fallo antes de generarlos.'
                }
            }
        }
    }
}
