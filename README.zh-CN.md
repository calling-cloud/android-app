# Telephone Android App

[English](README.md) | [中文](README.zh-CN.md)

Telephone 的 Android 客户端。本目录是仓库 `app` 目录下的独立 Gradle Android 工程。

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Navigation Compose
- AndroidX Lifecycle ViewModel
- Media3 ExoPlayer
- Aliyun OSS Android SDK

## 项目信息

- Gradle 根项目：`telephone`
- Android 模块：`:app`
- 包命名空间：`com.example.telephone`
- Application ID：`com.example.telephone`
- 最低 SDK：26
- 目标 SDK：36
- 编译 SDK：36
- 版本：`1.1.0` (`101000`)

## 服务地址

应用按构建类型使用不同的服务地址：

- Debug：`http://10.0.2.2:3000`
- Release：`http://47.109.29.124`

Debug 构建允许编辑服务地址，Release 构建不允许编辑。

## 主要功能

- 用户登录
- 拨号界面
- 通话记录
- 数据统计
- 个人资料
- 通话中服务集成
- 通话录音支持
- 待同步通话记录
- 应用更新安装流程
- OSS 分片上传

## 常用命令

在本目录下执行：

```sh
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
./gradlew :app:test
./gradlew :app:connectedAndroidTest
```

## 目录结构

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

## 关键文件

- `app/src/main/java/com/example/telephone/MainActivity.kt` - 应用入口 Activity
- `app/src/main/java/com/example/telephone/TelephoneInCallService.kt` - 通话中服务
- `app/src/main/java/com/example/telephone/data/ApiClient.kt` - 后端 API 客户端
- `app/src/main/java/com/example/telephone/ui/screens/` - Compose 页面
- `app/src/main/java/com/example/telephone/update/AppUpdateFlow.kt` - 应用更新流程
- `app/src/main/AndroidManifest.xml` - 权限、Activity、Service 和 Provider 配置

## 发布构建说明

Release 构建使用 `release` 签名配置。签名信息从 Gradle 项目属性读取：

- `RELEASE_STORE_FILE`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

不要把签名密钥或密码写入共享文档和源码变更。
