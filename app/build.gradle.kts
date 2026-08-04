import java.util.Properties
import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("template.android.application")
    id("template.android.compose")
    id("template.android.hilt")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

val localProps = Properties().also { props ->
    val f = rootProject.file("local.properties")
    if (f.exists()) props.load(f.inputStream())
}

android {
    namespace = "com.template.android"

    defaultConfig {
        applicationId = "com.template.android"
        versionCode = 1
        versionName = "1.0"
        // Inject secrets from local.properties — never hardcode keys in source.
        // Add your key to local.properties: api.key=<value>
        buildConfigField("String", "API_KEY", "\"${localProps.getProperty("api.key", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"https://dev.api.example.com/\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.example.com/\"")
        }
    }
}

dependencies {
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-datastore"))
    implementation(project(":layer:layer-data"))
    implementation(project(":feature:feature-home"))

    implementation(catalog.findLibrary("timber").get())
    implementation(catalog.findLibrary("androidx-core-splashscreen").get())
}
