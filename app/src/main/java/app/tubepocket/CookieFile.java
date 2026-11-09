package app.tubepocket;

import java.util.Locale;

/** Validates a user-selected Netscape cookie export without including values in errors. */
public final class CookieFile {
    public static final int MAX_BYTES = 1024 * 1024;
    public static final class Result {
        public final String text;
        public final int count;
        Result(String text, int count) { this.text = text; this.count = count; }
    }
    public static Result parse(String input, long nowSeconds) {
        if (input == null || input.length() > MAX_BYTES) throw invalid();
        StringBuilder output = new StringBuilder("# Netscape HTTP Cookie File\n\n");
        int count = 0, expired = 0;
        for (String raw : input.replace("\uFEFF", "").split("\\r?\\n")) {
            if (raw.isEmpty()) continue;
            boolean httpOnly = raw.startsWith("#HttpOnly_");
            if (raw.startsWith("#") && !httpOnly) continue;
            String line = httpOnly ? raw.substring("#HttpOnly_".length()) : raw;
            String[] fields = line.split("\t", -1);
            if (fields.length != 7) throw invalid();
            String domain = fields[0].toLowerCase(Locale.ROOT);
            String host = domain.startsWith(".") ? domain.substring(1) : domain;
            if (!host.equals("youtube.com") && !host.endsWith(".youtube.com")) continue;
            if (!domain.matches("\\.?[a-z0-9-]+(?:\\.[a-z0-9-]+)*")
                    || !(fields[1].equals("TRUE") || fields[1].equals("FALSE"))
                    || domain.startsWith(".") != fields[1].equals("TRUE")
                    || !(fields[3].equals("TRUE") || fields[3].equals("FALSE"))
                    || !fields[2].startsWith("/") || !printable(fields[2])
                    || !fields[5].matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+") || !printable(fields[6])) throw invalid();
            long expires = 0;
            if (!fields[4].isEmpty()) {
                if (!fields[4].matches("[0-9]+")) throw invalid();
                try { expires = Long.parseLong(fields[4]); } catch (NumberFormatException e) { throw invalid(); }
            }
            if (expires != 0 && expires <= nowSeconds) { expired++; continue; }
            if (++count > 500) throw invalid();
            fields[0] = domain;
            output.append(httpOnly ? "#HttpOnly_" : "").append(String.join("\t", fields)).append('\n');
        }
        if (count == 0) throw new IllegalArgumentException(expired > 0
                ? "YouTube Cookie 已过期，请在浏览器完成验证后重新导出"
                : "没有可用的 YouTube 会话，请从 YouTube 页面导出 Netscape 格式后重试");
        return new Result(output.toString(), count);
    }
    private static boolean printable(String value) {
        if (value.length() > 65536) return false;
        for (int i = 0; i < value.length(); i++) if (value.charAt(i) < 32 || value.charAt(i) > 126) return false;
        return true;
    }
    private static IllegalArgumentException invalid() { return new IllegalArgumentException("会话格式不正确，请在 Cookie-Editor 中选择 Netscape 格式导出（最大 1 MB）"); }
    private CookieFile() { }
}
