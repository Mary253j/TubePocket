package app.tubepocket;

import com.yausername.youtubedl_android.YoutubeDLRequest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Store under Android's noBackupFilesDir. Each engine request receives its own disposable jar. */
public final class CookieStore {
    private static final Object LOCK = new Object();
    private static final Set<File> OPEN = new HashSet<>();
    private final File directory, original;
    public CookieStore(File noBackupDirectory) {
        directory = new File(noBackupDirectory, "youtube-session");
        original = new File(directory, "cookies.txt");
    }
    public boolean hasSession() { synchronized (LOCK) { return original.isFile(); } }
    public int importFrom(InputStream source) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int length;
        while ((length = source.read(buffer)) != -1) {
            if (out.size() + length > CookieFile.MAX_BYTES) throw new IOException("Cookie 文件不能超过 1 MB");
            out.write(buffer, 0, length);
        }
        CookieFile.Result parsed = CookieFile.parse(out.toString(StandardCharsets.UTF_8.name()), System.currentTimeMillis() / 1000);
        synchronized (LOCK) {
            ensureDirectory();
            File pending = File.createTempFile("import-", ".txt", directory);
            try {
                makePrivate(pending);
                try (FileOutputStream file = new FileOutputStream(pending)) { file.write(parsed.text.getBytes(StandardCharsets.UTF_8)); file.getFD().sync(); }
                Files.move(pending.toPath(), original.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } finally { if (pending.exists()) pending.delete(); }
        }
        return parsed.count;
    }
    public Session attach(YoutubeDLRequest request) throws IOException {
        synchronized (LOCK) {
            if (!original.isFile()) return new Session(null);
            File copy = File.createTempFile("request-", ".txt", directory);
            try {
                makePrivate(copy);
                try (InputStream in = new FileInputStream(original); OutputStream out = new FileOutputStream(copy)) {
                    byte[] buffer = new byte[8192]; int length; while ((length = in.read(buffer)) != -1) out.write(buffer, 0, length);
                }
                request.addOption("--cookies", copy.getAbsolutePath());
                OPEN.add(copy); return new Session(copy);
            } catch (IOException | RuntimeException e) { copy.delete(); throw e; }
        }
    }
    public void clear() throws IOException {
        synchronized (LOCK) {
            for (File file : OPEN) if (directory.equals(file.getParentFile())) throw new IOException("当前任务正在使用登录凭据，请完成或取消任务后再清除");
            File[] files = directory.listFiles();
            if (files != null) for (File file : files) if (file.isFile() && !file.delete()) throw new IOException("未能清除登录凭据，请重试");
        }
    }
    public void cleanupStaleCopies() {
        synchronized (LOCK) {
            File[] files = directory.listFiles();
            if (files != null) for (File file : files)
                if ((file.getName().startsWith("request-") || file.getName().startsWith("import-")) && !OPEN.contains(file)) file.delete();
        }
    }
    private void ensureDirectory() throws IOException { if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法保存登录凭据"); }
    private static void makePrivate(File file) throws IOException {
        if (!file.setReadable(false, false) || !file.setWritable(false, false) || !file.setReadable(true, true) || !file.setWritable(true, true)) throw new IOException("无法设置凭据文件权限");
    }
    public static final class Session implements AutoCloseable {
        private final File file;
        Session(File file) { this.file = file; }
        @Override public void close() {
            synchronized (LOCK) { if (file != null) { OPEN.remove(file); file.delete(); } }
        }
    }
}
