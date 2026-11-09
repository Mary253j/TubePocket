# 拾光 / TubePocket 1.0.3

中文原生 Android 视频下载器，支持 YouTube 普通视频、短链接及 Shorts。粘贴链接、解析、选择画质并下载到系统 Download/TubePocket，可后台下载、取消、重试、播放和分享，无需自建服务器。

## GitHub 发布与隐私检查

APK 将放在本仓库的 Releases 页面。请下载发布附件中的安装包；源码仅用于审阅和自行构建。个人签名私钥未上传，未来在其他机器重新生成的签名可能无法覆盖现有安装。

发布前检查结果见 [SECURITY_AUDIT.md](SECURITY_AUDIT.md)。

## 安装与基本使用

安装 TubePocket-1.0.3-arm64.apk，适用于 Android 8.0 以上的 ARM64 手机。应用 ID 和签名与前版一致，可直接覆盖安装、保留下载记录。交付为关闭调试的 Release 包，使用原测试签名，未上架应用商店。

粘贴链接 → 解析视频 → 选择画质 → 下载到手机。也可以在 YouTube 点“分享”，选择拾光。画质数字为上限，实际使用上限内的最佳可用画质；MP4 是容器，4K/AV1 等编码需播放器支持。手机网络须能访问 YouTube。

应用不添加水印，也不移除原视频已有水印。仅保存你有权下载的内容。不支持播放列表、进行中的直播、会员付费或 DRM 视频。

## 只用手机处理登录 / 人机验证

1. 在最新版 Edge 安卓版安装 Cookie-Editor 扩展。
2. 用 Edge 打开该 YouTube 视频，自行登录并完成验证，确认能播放。
3. 在视频页打开扩展，选择 Export → Netscape，复制当前网站的会话。
4. 返回拾光，点“设置 → 粘贴手机浏览器会话”，再重新解析。

应用的“手机登录 / 验证”入口包含安装页、扩展页、打开视频与粘贴按钮。全程手机完成，无需电脑或 TXT 文件。具体步骤、官方链接和验证范围见 MOBILE_SETUP.md。仍保留 Netscape 文件导入功能。

Cookie 含登录凭据，不应发到聊天中或交给他人。应用只保留 YouTube Cookie，拒绝格式错误或全数过期的内容。副本保存在本机 noBackupFilesDir 的私有目录，不进入系统备份或公共下载文件夹；每个请求使用独立临时副本，结束后删除。支持手动清除。粘贴导入成功后清除剪贴板中未发生变化的同一份会话。YouTube 仍可能拒绝请求。

链接与下载记录保存在本机，无自建服务器、广告或统计 SDK。更新引擎时连接 GitHub。Edge、Cookie-Editor 由用户另行安装并授权，未打包进应用。

## 构建

需要 JDK 17 或 21、Android SDK Platform 35、Build Tools 35.0.0，以及可访问 Google Maven、Maven Central、Gradle 分发服务的网络。在 local.properties 配置本机 sdk.dir，或用 Android Studio 打开并同步。

```sh
./gradlew :app:assembleRelease :app:testDebugUnitTest :app:lintRelease
```

输出：

- app/build/outputs/apk/release/app-arm64-v8a-release.apk
- app/build/outputs/apk/release/app-x86_64-release.apk

源码不含签名私钥。自行构建会使用本机 debug key，通常不能覆盖本次提供的 APK；可另设 applicationId 或使用测试设备。正式分发应使用自己的签名并完成目标机型和真实网络测试。

设备测试（使用本地视频和虚构凭据，不请求远程视频）：

```sh
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=app.tubepocket.DeviceSmokeTest,app.tubepocket.CookieDeviceTest
```

## 引擎与许可

youtubedl-android 和 FFmpeg 封装版本 0.18.1，来自 Maven Central。应用资源覆盖的 yt-dlp 为官方 2026.10.26 zipapp，内含 EJS：

- https://github.com/yt-dlp/yt-dlp/releases/download/2026.10.26/yt-dlp
- SHA-256: e51e69e89489f199530f13d32a4342da3ddb2cf34842f364603a066db05afbf3

完整许可及组件源码信息见 LICENSE、THIRD_PARTY_NOTICES.md 与 third_party/。本次验证结果及限制见 TEST_REPORT.md。
