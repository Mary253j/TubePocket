package app.tubepocket;

import java.util.Locale;

public final class ErrorMessages {
    public static boolean needsVerification(String message) {
        String lower = String.valueOf(message).toLowerCase(Locale.ROOT);
        return lower.contains("not a bot") || lower.contains("captcha") || lower.contains("unusual traffic") || lower.contains("automated queries");
    }
    public static boolean needsSession(String message) {
        String lower = String.valueOf(message).toLowerCase(Locale.ROOT);
        return needsVerification(message) || lower.contains("sign in") || lower.contains("login") || lower.contains("age-restricted");
    }
    public static String friendly(Throwable error) {
        String message = String.valueOf(error.getMessage());
        String lower = message.toLowerCase(Locale.ROOT);
        if (needsVerification(message)) return "YouTube 要求人机验证，请在浏览器验证后导入登录凭据";
        if (lower.contains("space") || lower.contains("enospc")) return "存储空间不足，请清理后重试";
        if (needsSession(message)) return "YouTube 要求登录，可在设置中导入你自己的登录凭据";
        if (lower.contains("private video")) return "该视频未公开，请确认你具有访问权限";
        if (lower.contains("unavailable") || lower.contains("not available") || lower.contains("removed")) return "视频已下架或在当前地区不可用";
        if (lower.contains("403") || lower.contains("429")) return "YouTube 暂时拒绝请求，请稍后重试或更新解析引擎";
        if (lower.contains("unable to download") || lower.contains("timed out") || lower.contains("network") || lower.contains("resolve") || lower.contains("connection")) return "无法连接，请检查手机网络能否访问 YouTube";
        if (lower.contains("format") || lower.contains("javascript") || lower.contains("signature")) return "视频格式解析失败，请在设置中更新解析引擎后重试";
        return "处理失败，请重试；可在详情中查看错误信息";
    }
    public static String safeDetails(String message) {
        StringBuilder out = new StringBuilder();
        for (String line : String.valueOf(message).split("\\R")) {
            String lower = line.trim().toLowerCase(Locale.ROOT);
            if (lower.contains("skipping cookie file entry") || lower.startsWith("cookie:") || lower.startsWith("authorization:")
                    || lower.contains("sapisid=") || lower.contains("login_info=") || lower.contains("__secure-1psid=") || lower.contains("__secure-3psid=")) out.append("[登录凭据信息已隐藏]");
            else out.append(line);
            out.append('\n');
            if (out.length() > 8000) break;
        }
        return out.length() > 8000 ? out.substring(0, 8000) + "\n（已截断）" : out.toString().trim();
    }
    private ErrorMessages() { }
}
