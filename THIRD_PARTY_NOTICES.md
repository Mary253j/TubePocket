# 第三方组件与源码

拾光的原创代码使用 GPL-3.0-or-later，完整文本见根目录 `LICENSE`。各依赖保留其自身许可。本项目不声称拥有 YouTube、yt-dlp 或 FFmpeg 的商标。

| 组件 | 版本 / 来源 | 许可与对应源码 |
| --- | --- | --- |
| youtubedl-android library / common / ffmpeg | 0.18.1 | GPL-3.0；<https://github.com/yausername/youtubedl-android/tree/0.18.1> |
| yt-dlp zipapp | 2026.10.26 | Unlicense，打包组件另见第三方许可；<https://github.com/yt-dlp/yt-dlp/tree/2026.10.26> |
| QuickJS（由 Android 库打包） | 见上游构建 | MIT；<https://bellard.org/quickjs/>、<https://github.com/yausername/youtubedl-android/tree/0.18.1> |
| Python（由 Android 库打包） | 见上游构建 | PSF；<https://github.com/python/cpython>，构建说明见 `third_party/BUILD_PYTHON.md` |
| FFmpeg 及其原生依赖 | youtubedl-android 0.18.1 提供的原样二进制 | GPL/LGPL 取决于上游配置；<https://ffmpeg.org/download.html>；构建说明见 `third_party/BUILD_FFMPEG.md`，上游 Termux 包构建定义：<https://github.com/termux/termux-packages> |
| Kotlin stdlib | 1.9.24 | Apache-2.0；<https://github.com/JetBrains/kotlin/tree/v1.9.24> |
| AndroidX Core | 1.15.0 | Apache-2.0；<https://android.googlesource.com/platform/frameworks/support/> |
| AndroidX AppCompat 等传递依赖 | 由 Gradle 解析 | Apache-2.0；同上 |
| Jackson | 2.11.1（上游传递依赖） | Apache-2.0；<https://github.com/FasterXML/jackson> |
| Apache Commons IO | 2.5（上游传递依赖） | Apache-2.0；<https://commons.apache.org/proper/commons-io/> |
| JUnit / AndroidX Test | 仅测试 | EPL-1.0 / Apache-2.0；不打包到用户主 APK |

`third_party/YTDLP-LICENSE.txt` 与 `third_party/YTDLP-THIRD-PARTY.txt` 是官方发布版本的许可文本。原生二进制及其依赖未由本项目修改，来源为固定版本的 Maven AAR；相应构建脚本在上游仓库及上述构建说明中。

可以通过 `./gradlew :app:dependencies --configuration debugRuntimeClasspath` 查看完整依赖树。源码包提供应用的完整源代码、构建文件、Gradle wrapper 和所覆盖的 yt-dlp 资源。
