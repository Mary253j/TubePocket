# 1.0.3 验证结果

- assembleRelease、testDebugUnitTest、lintRelease 均构建成功。
- 15 项 JVM 回归测试通过；本版主要调整浏览器路由、扩展商店链接及使用引导，凭据导入与下载引擎逻辑不变。
- 已核对 Cookie-Editor 官方说明，列明 Edge Android 支持；已确认 Microsoft Edge Add-ons 的 Cookie-Editor 页面，以及 Microsoft Edge 官方移动页面介绍扩展功能。
- APK 签名验证和 ZIP 对齐验证通过。应用 ID 不变，versionCode 提高为 4，签名与既有交付版本相同，支持覆盖安装。
- Release 构建关闭调试，私钥未包含在源码中。

## 限制

没有完成 Edge 实际登录、扩展安装、会话复制及真实 YouTube 下载的设备端验证。开发环境访问 YouTube 仍受代理 403 限制。既有模拟器的系统服务异常，不作为本版流程验证通过的依据。

1.0.0 的 Android 11 / x86_64 基线曾通过 3 项设备测试，原始记录见 verification/baseline-1.0.0-device-tests.txt。

是否能安装扩展以用户 Edge 显示为准。登录和验证码由用户在正常浏览器中手动完成；导入会话不保证 YouTube 接受请求。

手机 Edge 步骤及官方链接见 MOBILE_SETUP.md，APK 摘要和测试计数见 verification/build-verification.json。
