# Telephone Android App

[English](README.md) | [中文](README.zh-CN.md)

Android client for Telephone. This module is a standalone Gradle Android project under the repository `app` directory.

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Navigation Compose
- AndroidX Lifecycle ViewModel
- Media3 ExoPlayer
- Aliyun OSS Android SDK

## Project Info

- Gradle root project: `telephone`
- Android module: `:app`
- Package namespace: `com.example.telephone`
- Application ID: `com.example.telephone`
- Minimum SDK: 26
- Target SDK: 36
- Compile SDK: 36
- Version: `1.1.0` (`101000`)

## Server URLs

The app uses build-type specific server URLs:

- Debug: `http://10.0.2.2:3000`
- Release: `http://47.109.29.124`

Debug builds allow editing the server URL. Release builds do not.

## Main Features

- User login
- Dialer UI
- Call records
- Stats screen
- Profile screen
- In-call service integration
- Call recording support
- Pending call sync
- App update installation flow
- OSS multipart upload

## Common Commands

Run commands from this directory:

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:test
./gradlew :app:connectedAndroidTest
```

## Directory Layout

```text
app/
  build.gradle.kts
  gradle/libs.versions.toml
  app/
    build.gradle.kts
    src/
      main/
        AndroidManifest.xml
        java/com/example/telephone/
        res/
      test/
      androidTest/
```

## Important Files

- `app/src/main/java/com/example/telephone/MainActivity.kt` - app entry activity
- `app/src/main/java/com/example/telephone/TelephoneInCallService.kt` - in-call service
- `app/src/main/java/com/example/telephone/data/ApiClient.kt` - backend API client
- `app/src/main/java/com/example/telephone/ui/screens/` - Compose screens
- `app/src/main/java/com/example/telephone/update/AppUpdateFlow.kt` - app update flow
- `app/src/main/AndroidManifest.xml` - permissions, activities, services, and providers

## Release Build Notes

Release builds use the `release` signing config. The required signing properties are read from Gradle project properties:

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

Keep signing secrets out of shared documentation and source changes.
