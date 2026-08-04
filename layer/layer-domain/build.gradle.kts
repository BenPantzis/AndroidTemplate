import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("template.android.library")
}

val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

android {
    namespace = "com.template.android.core.domain"
}

dependencies {
    api(project(":core:core-common"))
    api(catalog.findLibrary("coroutines-android").get())
    compileOnly("javax.inject:javax.inject:1")
}
