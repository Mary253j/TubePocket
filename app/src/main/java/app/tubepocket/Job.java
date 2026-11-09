package app.tubepocket;

import org.json.JSONObject;

public final class Job {
    public String id, url, title, state = "queued", detail = "等待下载", uri = "", fileName = "", rawError = "";
    public int height, progress;
    public long created = System.currentTimeMillis();
    public Job(String id, String url, String title, int height) { this.id = id; this.url = url; this.title = title; this.height = height; }
    public Job(JSONObject json) {
        id = json.optString("id"); url = json.optString("url"); title = json.optString("title");
        state = json.optString("state", "interrupted"); detail = json.optString("detail"); uri = json.optString("uri");
        fileName = json.optString("fileName"); rawError = json.optString("rawError"); height = json.optInt("height");
        progress = json.optInt("progress"); created = json.optLong("created");
    }
    public JSONObject json() {
        JSONObject value = new JSONObject();
        try {
            value.put("id", id).put("url", url).put("title", title).put("state", state).put("detail", detail)
                .put("uri", uri).put("fileName", fileName).put("rawError", rawError).put("height", height).put("progress", progress).put("created", created);
        } catch (Exception ignored) { }
        return value;
    }
    public Job copy() { return new Job(json()); }
}
