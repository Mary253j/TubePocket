package app.tubepocket;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Accepts only individual YouTube video links. Never forwards arbitrary URLs to the engine. */
public final class VideoLink {
    private static final Pattern LINK = Pattern.compile("(?i)(?<![\\p{Alnum}._/@-])(?:https?://)?(?:(?:www|m|music)\\.)?(?:youtube\\.com|youtu\\.be)/[^\\s<>\\\"，。；]+ ".trim());
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    public static String normalize(String text) {
        if (text == null) throw new IllegalArgumentException("请先粘贴 YouTube 视频链接");
        Matcher matcher = LINK.matcher(text.trim());
        while (matcher.find()) {
            String candidate = matcher.group().replaceAll("[)\\]}>.,;!]+$", "");
            if (!candidate.matches("(?i)^https?://.*")) candidate = "https://" + candidate;
            try {
                URI uri = URI.create(candidate);
                String host = uri.getHost();
                if (host == null || uri.getUserInfo() != null || uri.getPort() != -1) continue;
                host = host.toLowerCase(java.util.Locale.ROOT);
                String id = null;
                String path = uri.getPath();
                if (host.equals("youtu.be")) {
                    id = path.substring(1);
                } else if (host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com") || host.equals("music.youtube.com")) {
                    if (path.equals("/watch") && uri.getRawQuery() != null) {
                        for (String pair : uri.getRawQuery().split("&")) {
                            String[] parts = pair.split("=", 2);
                            if (parts.length == 2 && parts[0].equals("v")) id = URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
                        }
                    } else {
                        String[] parts = path.split("/");
                        if (parts.length == 3 && (parts[1].equals("shorts") || parts[1].equals("live") || parts[1].equals("embed"))) id = parts[2];
                    }
                }
                if (id != null && ID.matcher(id).matches()) return "https://www.youtube.com/watch?v=" + id;
            } catch (Exception ignored) { }
        }
        throw new IllegalArgumentException("链接无效，请使用单个 YouTube 视频或 Shorts 链接");
    }
    private VideoLink() { }
}
