package app.tubepocket;

public final class DownloadPolicy {
    public static String format(int height) {
        String limit = height > 0 ? "[height<=" + height + "]" : "";
        return "bv*" + limit + "+ba/b" + limit;
    }
    public static String quality(int height) { return height == 0 ? "最佳画质" : "最高 " + height + "p"; }
    public static boolean cancellable(String state) { return state.equals("queued") || state.equals("running"); }
    public static boolean retryable(String state) { return state.equals("failed") || state.equals("interrupted") || state.equals("cancelled"); }
    private DownloadPolicy() { }
}
