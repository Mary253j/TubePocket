# 只用手机 Edge 完成登录和验证

整个流程不需要电脑、Firefox 或 TXT 文件。需要 Edge 安卓版提供“扩展 / 扩展程序”入口，并允许安装 Cookie-Editor。是否能安装以 Edge 显示为准；缺少扩展入口时先更新 Edge。

1. 在 Edge 菜单打开“扩展 / 扩展程序”，搜索并安装 Cookie-Editor。官方扩展页：
   https://microsoftedge.microsoft.com/addons/detail/cookieeditor/neaplmfkghagebokkhpjpoebhdledlfi
2. 用 Edge 打开需要保存的 YouTube 视频，自行登录并完成页面要求的验证，确认视频能播放。
3. 留在 YouTube 视频页，打开 Edge 菜单 → 扩展 → Cookie-Editor，点 Export，选择 Netscape。会话会复制到剪贴板。
4. 返回拾光，点“设置 → 粘贴手机浏览器会话”，或错误提示中的“已复制，粘贴会话”，再重新解析视频。

应用的“手机登录 / 验证”引导和打开视频、扩展页按钮均优先使用 Edge，支持常见发行渠道。Edge 官方更新页：
https://www.microsoft.com/zh-cn/edge/mobile

Cookie 含登录凭据，仅使用自己的会话。不要将会话发到聊天中或交给他人。应用仅在主动点粘贴时读取剪贴板，只保存 YouTube 域名的记录；导入成功后清除剪贴板中未变化的同一份会话。应用保存的副本可在设置中清除。

Cookie-Editor 由你在 Edge 内安装并授权，没有打包进拾光。其官方说明列出 Edge Android 支持，Microsoft Edge 官方移动页面也介绍扩展功能：

- https://github.com/Moustachauve/cookie-editor
- https://microsoftedge.microsoft.com/addons/detail/cookieeditor/neaplmfkghagebokkhpjpoebhdledlfi
- https://www.microsoft.com/zh-cn/edge/mobile

实际手机登录和下载尚未在开发环境验证。YouTube 仍可能因网络、账号或其他限制拒绝请求，导入会话不保证下载成功。
