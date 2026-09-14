pipeline {
    agent any

    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
    }

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

        stage('Secretos') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    powershell '.\\scripts\\jenkins\\Invoke-Gitleaks.ps1'
                }
            }
        }

        stage('Calidad Kotlin') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    bat 'gradlew.bat ktlintCheck detekt testDebugUnitTest --continue'
                }
            }
        }

        stage('Lint (Android)') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    bat '"%JAVA_HOME%\\bin\\java.exe" -version'
                    bat 'gradlew.bat --version'
                    bat 'gradlew.bat lintDebug'
                }
            }
        }

        stage('Dependencias vulnerables') {
            steps {
                catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                    withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                        bat 'gradlew.bat --no-configuration-cache :app:dependencyCheckAnalyze'
                    }
                }
            }
        }

        stage('Construir APK') {
            when {
                expression { currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                bat 'gradlew.bat :app:stageDebugApk -PciBuildNumber=%BUILD_NUMBER%'
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

                archiveArtifacts(
                    artifacts: 'app/build/outputs/jenkins/*.apk,app/build/reports/**,build/reports/**',
                    allowEmptyArchive: true,
                    fingerprint: true
                )
            }
        }
    }
}
