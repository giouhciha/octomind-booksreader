import dev.detekt.gradle.Detekt
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension
import org.owasp.dependencycheck.reporting.ReportGenerator

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
    id("org.owasp.dependencycheck")
}

val baseVersionCode = 65
val baseVersionName = "0.54.0"
val ciBuildNumber = providers.gradleProperty("ciBuildNumber").orNull?.toIntOrNull()
require(ciBuildNumber == null || ciBuildNumber in 1..99_999) {
    "ciBuildNumber debe ser un entero entre 1 y 99999."
}
val effectiveVersionCode = ciBuildNumber?.let { baseVersionCode * 100_000 + it } ?: baseVersionCode
val effectiveVersionName = ciBuildNumber?.let { "$baseVersionName.$it" } ?: baseVersionName

android {
    namespace = "com.octomind.booksreader"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.octomind.booksreader"
        minSdk = 26
        targetSdk = 37
        versionCode = effectiveVersionCode
        versionName = effectiveVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

tasks.register<Sync>("stageDebugApk") {
    dependsOn("assembleDebug")
    from(layout.buildDirectory.file("outputs/apk/debug/app-debug.apk"))
    into(layout.buildDirectory.dir("outputs/jenkins"))
    rename("app-debug\\.apk", "octomind-booksreader-$effectiveVersionName.apk")
}

ktlint {
    android.set(true)
    baseline.set(file("config/ktlint/baseline.xml"))
    reporters {
        reporter(ReporterType.PLAIN)
        reporter(ReporterType.CHECKSTYLE)
    }
    filter {
        exclude("**/generated/**")
    }
}

detekt {
    buildUponDefaultConfig = true
    baseline = file("config/detekt/baseline.xml")
    parallel = true
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        sarif.required.set(true)
    }
}

configure<DependencyCheckExtension> {
    format = ReportGenerator.Format.ALL.toString()
    failBuildOnCVSS = 7.0f
    scanConfigurations = listOf("debugRuntimeClasspath", "releaseRuntimeClasspath")
    suppressionFile = file("config/dependency-check/suppressions.xml").absolutePath
    analyzers.assemblyEnabled = false
    System.getenv("NVD_API_KEY")?.takeIf { it.isNotBlank() }?.let { nvd.apiKey = it }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.08.00")

    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
}
