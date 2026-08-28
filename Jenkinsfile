pipeline {
    agent any

    environment {
        JAVA_HOME = 'C:\\Program Files\\Android\\Android Studio\\jbr'
        ANDROID_HOME = 'C:\\Users\\gio_u\\AppData\\Local\\Android\\Sdk'
        ANDROID_AVD_HOME = 'C:\\Users\\gio_u\\.android\\avd'
        ANDROID_USER_HOME = 'C:\\ProgramData\\Jenkins\\.jenkins\\.android'
        ADB_VENDOR_KEYS = 'C:\\ProgramData\\Jenkins\\.jenkins\\.android'
        ANDROID_ADB_SERVER_PORT = '5038'
        ANDROID_SERIAL = 'emulator-5554'
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

        stage('Formato Kotlin') {
            steps {
                bat 'gradlew.bat ktlintCheck'
            }
        }

        stage('Analisis estatico') {
            steps {
                bat 'gradlew.bat detekt'
            }
        }

        stage('Secretos') {
            steps {
                powershell '.\\scripts\\jenkins\\Invoke-Gitleaks.ps1'
            }
        }

        stage('Dependencias vulnerables') {
            steps {
                withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                    bat 'gradlew.bat --no-configuration-cache :app:dependencyCheckAnalyze'
                }
            }
        }

        stage('Unit Tests') {
            steps {
                bat 'gradlew.bat testDebugUnitTest'
            }
        }

        stage('Iniciar emulador') {
            steps {
                powershell '.\\scripts\\jenkins\\Start-AndroidEmulator.ps1'
            }
        }

        stage('Pruebas UI') {
            steps {
                bat 'gradlew.bat :app:connectedDebugAndroidTest'
            }
        }

        stage('Instalacion y apertura') {
            steps {
                powershell '.\\scripts\\jenkins\\Verify-AppLaunch.ps1'
            }
        }
    }

    post {
        always {
            script {
                powershell(returnStatus: true, script: '.\\scripts\\jenkins\\Stop-AndroidEmulator.ps1')

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

                if (fileExists('app/build/outputs/androidTest-results/connected/debug')) {
                    junit testResults: 'app/build/outputs/androidTest-results/connected/debug/TEST-*.xml'
                } else {
                    echo 'No hay resultados UI porque la etapa no se ejecuto o fallo antes de generarlos.'
                }

                archiveArtifacts(
                    artifacts: 'app/build/reports/**,build/reports/**',
                    allowEmptyArchive: true
                )
            }
        }
    }
}
