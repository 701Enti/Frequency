pluginManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            name = "Aliyun Gradle Plugin"
        }
        maven {
            url = uri("https://www.jitpack.io")
            name = "JitPack"
        }
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven {
            url = uri("https://maven.aliyun.com/repository/public")
            name = "Aliyun Public"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/google")
            name = "Aliyun Google"
        }
        maven {
            url = uri("https://maven.aliyun.com/repository/gradle-plugin")
            name = "Aliyun Gradle Plugin"
        }
        maven {
            url = uri("https://jitpack.io")
            name = "JitPack"
        }
        google()
        gradlePluginPortal()
    }
}

plugins{
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "Frealicane"
include (":app")
include (":BluetoothFocuser")
