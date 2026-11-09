package app.tubepocket;

import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.core.content.FileProvider;
import java.io.*;

public final class VideoStorage {
    public static Uri publish(Context context, Job job, File source) throws IOException {
        String name = job.title.replaceAll("[\\p{Cntrl}/\\\\:*?\"<>|]", "_").trim();
        if (name.isEmpty()) name = "YouTube 视频";
        if (name.length() > 80) name = name.substring(0, 80);
        name += "-" + job.id.substring(0, 6) + ".mp4";
        final String fileName = name;
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/TubePocket");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IOException("无法创建下载文件");
            PocketApp.instance.jobs.edit(job.id, j -> { j.uri = uri.toString(); j.fileName = fileName; });
            try {
                try (InputStream in = new FileInputStream(source); OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new IOException("无法打开下载文件");
                    copy(in, out);
                }
                values.clear(); values.put(MediaStore.MediaColumns.IS_PENDING, 0);
                if (context.getContentResolver().update(uri, values, null, null) != 1) throw new IOException("无法完成文件保存");
                return uri;
            } catch (Exception e) {
                context.getContentResolver().delete(uri, null, null);
                if (e instanceof IOException) throw (IOException)e;
                throw new IOException(e);
            }
        }
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "TubePocket");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("无法创建下载文件夹");
        File destination = new File(directory, name);
        File pending = new File(directory, "." + name + ".pending");
        try {
            try (InputStream in = new FileInputStream(source); OutputStream out = new FileOutputStream(pending)) { copy(in, out); }
            if (!pending.renameTo(destination)) throw new IOException("无法完成文件保存");
        } finally { if (pending.exists()) pending.delete(); }
        MediaScannerConnection.scanFile(context, new String[]{destination.getAbsolutePath()}, new String[]{"video/mp4"}, null);
        PocketApp.instance.jobs.edit(job.id, j -> j.fileName = fileName);
        return FileProvider.getUriForFile(context, context.getPackageName() + ".files", destination);
    }
    private static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[128 * 1024]; int length;
        while ((length = in.read(buffer)) != -1) {
            if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("保存被中断");
            out.write(buffer, 0, length);
        }
        out.flush();
    }
    public static void deleteTree(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        file.delete();
    }
    private VideoStorage() { }
}
