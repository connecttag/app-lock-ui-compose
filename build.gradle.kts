plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose.plugin)
    `maven-publish`
}

// JitPack supplies transport coordinates; normal Maven publishing keeps the
// canonical ConnectTag identity declared in gradle.properties.
val publicationArtifact = providers.environmentVariable("ARTIFACT")
    .orElse(providers.gradleProperty("LIB_ARTIFACT_ID")).get()
group = providers.gradleProperty("group")
    .orElse(providers.environmentVariable("GROUP"))
    .orElse(providers.gradleProperty("LIB_GROUP")).get()
version = providers.gradleProperty("version")
    .orElse(providers.environmentVariable("VERSION"))
    .orElse(providers.gradleProperty("LIB_VERSION")).get()

val isJitPackBuild = providers.environmentVariable("JITPACK").isPresent ||
    group.toString().startsWith("com.github.")

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

android {
    namespace = "org.connecttag.lib.features.applock.ui"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    if (isJitPackBuild) {
        api("com.github.connecttag:app-lock-android:1.1.0")
    } else {
        api("org.connecttag.lib:feature-app-lock-android:1.1.0")
    }
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.compose.animation.android)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit4)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = publicationArtifact
                version = project.version.toString()

                pom {
                    name.set("ConnectTag App Lock UI Compose")
                    description.set("Jetpack Compose Material3 UI suite for ConnectTag App Lock: anti-spy Fisher-Yates scrambled PinPad, PinLockScreen, AppLockGate, and settings components")
                    url.set("https://github.com/connecttag/app-lock-ui-compose")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("connecttagye")
                            name.set("Connect Tag")
                            email.set("connecttagye@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/connecttag/app-lock-ui-compose.git")
                        developerConnection.set("scm:git:ssh://git@github.com:connecttag/app-lock-ui-compose.git")
                        url.set("https://github.com/connecttag/app-lock-ui-compose/tree/main")
                    }
                }
            }
        }
    }
}
