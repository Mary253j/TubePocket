package app.tubepocket;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class JobStore {
    private final SharedPreferences prefs;
    private final ArrayList<Job> jobs = new ArrayList<>();
    public JobStore(Context context) {
        prefs = context.getSharedPreferences("downloads", Context.MODE_PRIVATE);
        try {
            JSONArray list = new JSONArray(prefs.getString("jobs", "[]"));
            for (int i = 0; i < list.length(); i++) {
                Job job = new Job(list.getJSONObject(i));
                if (job.state.equals("queued") || job.state.equals("running") || job.state.equals("saving")) {
                    if (job.state.equals("saving") && job.uri.startsWith("content://media/")) {
                        try { context.getContentResolver().delete(android.net.Uri.parse(job.uri), null, null); } catch (Exception ignored) { }
                        job.uri = "";
                    }
                    job.state = "interrupted"; job.detail = "下载被系统中断，点击重试";
                }
                jobs.add(job);
            }
        } catch (Exception ignored) { }
        save();
    }
    public synchronized List<Job> all() { List<Job> out = new ArrayList<>(); for (Job job : jobs) out.add(job.copy()); return out; }
    public synchronized Job get(String id) { for (Job job : jobs) if (job.id.equals(id)) return job.copy(); return null; }
    public synchronized void add(Job job) { jobs.add(0, job.copy()); save(); }
    public synchronized void edit(String id, Consumer<Job> change) {
        for (Job job : jobs) if (job.id.equals(id)) { change.accept(job); save(); return; }
    }
    public synchronized boolean beginSaving(String id) {
        for (Job job : jobs) if (job.id.equals(id) && job.state.equals("running")) {
            job.state = "saving"; job.detail = "正在保存到系统下载文件夹"; save(); return true;
        }
        return false;
    }
    public synchronized boolean cancel(String id) {
        for (Job job : jobs) if (job.id.equals(id) && DownloadPolicy.cancellable(job.state)) {
            job.state = "cancelled"; job.detail = "已取消"; save(); return true;
        }
        return false;
    }
    public synchronized boolean hasActive() { for (Job job : jobs) if (job.state.equals("queued") || job.state.equals("running") || job.state.equals("saving")) return true; return false; }
    private void save() { JSONArray list = new JSONArray(); for (Job job : jobs) list.put(job.json()); prefs.edit().putString("jobs", list.toString()).apply(); }
}
