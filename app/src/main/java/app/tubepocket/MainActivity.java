package app.tubepocket;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private static final String EDGE_ANDROID = "https://www.microsoft.com/zh-cn/edge/mobile";
    private static final String COOKIE_EDITOR = "https://microsoftedge.microsoft.com/addons/detail/cookieeditor/neaplmfkghagebokkhpjpoebhdledlfi";
    private static final int BG = 0xFFF5F6F0, INK = 0xFF183C30, GREEN = 0xFF28694E, MUTED = 0xFF748078, LINE = 0xFFE4E8DF, LIME = 0xFFD9EDAC;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService network = Executors.newFixedThreadPool(2);
    private LinearLayout previewBox, jobsBox;
    private TextView parseButton, countLabel, pasteButton;
    private EditText linkInput;
    private JSONObject preview;
    private String parsedUrl = "", previewProcess = "", jobsSignature = "";
    private boolean parsing, updating, importing;
    private Future<?> previewFuture;
    private Bitmap cachedThumbnail;
    private String cachedThumbnailUrl = "";
    private int height = 1080;
    private Job pendingJob;
    private final Runnable refresh = new Runnable() { @Override public void run() { renderJobs(); main.postDelayed(this, 800); } };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        if (state != null) {
            height = state.getInt("height", 1080); parsedUrl = state.getString("parsedUrl", "");
            try { if (state.containsKey("preview")) preview = new JSONObject(state.getString("preview")); } catch (Exception ignored) { }
            try { if (state.containsKey("pendingJob")) pendingJob = new Job(new JSONObject(state.getString("pendingJob"))); } catch (Exception ignored) { }
        }
        buildUi();
        if (state != null) linkInput.setText(state.getString("link", ""));
        if (preview != null) showPreview();
        else previewBox.setVisibility(View.GONE);
        if (state == null) receiveShare(getIntent());
    }

    private void buildUi() {
        LinearLayout root = column(); root.setBackgroundColor(BG);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom); return windowInsets;
        });
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setClipToPadding(false);
        LinearLayout content = column(); content.setPadding(dp(22), dp(14), dp(22), dp(24));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, -1)); scroll.addView(content);
        setContentView(root);

        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL);
        Icon logo = new Icon("download", LIME); logo.setBackground(shape(INK, 14));
        header.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout brand = column(); LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0, -2, 1); brandParams.leftMargin = dp(12);
        header.addView(brand, brandParams); brand.addView(text("拾光", 25, INK, true));
        TextView wordmark = text("T U B E  P O C K E T", 9, MUTED, false); wordmark.setPadding(0, dp(3), 0, 0); brand.addView(wordmark);
        Icon settings = new Icon("settings", INK); settings.setContentDescription("设置与手机登录"); settings.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES); settings.setBackground(shape(0xFFEAEDE4, 15));
        settings.setOnClickListener(v -> settings()); header.addView(settings, new LinearLayout.LayoutParams(dp(44), dp(44)));
        content.addView(header); space(content, 24);

        LinearLayout hero = column(); hero.setPadding(dp(22), dp(22), dp(22), dp(22)); hero.setBackground(shape(INK, 24));
        TextView eyebrow = text("SAVE IT. KEEP IT.", 10, LIME, true); eyebrow.setLetterSpacing(.15f); hero.addView(eyebrow);
        LinearLayout heroMain = row(); heroMain.setGravity(Gravity.CENTER_VERTICAL);
        TextView headline = text("喜欢的瞬间，\n随时回看。", 28, 0xFFFFFFFF, true); headline.setLineSpacing(dp(5), 1);
        heroMain.addView(headline, new LinearLayout.LayoutParams(0, -2, 1));
        Icon heroIcon = new Icon("hero", LIME); heroMain.addView(heroIcon, new LinearLayout.LayoutParams(dp(64), dp(78)));
        LinearLayout.LayoutParams heroParams = new LinearLayout.LayoutParams(-1, -2); heroParams.topMargin = dp(15); hero.addView(heroMain, heroParams);
        TextView description = text("一个链接，把视频收藏到手机。", 12, 0xFFBCD0C2, false); description.setPadding(0, dp(14), 0, 0); hero.addView(description);
        content.addView(hero); space(content, 16);

        LinearLayout inputCard = card();
        LinearLayout inputHeader = row(); inputHeader.setGravity(Gravity.CENTER_VERTICAL);
        Icon link = new Icon("link", GREEN); inputHeader.addView(link, new LinearLayout.LayoutParams(dp(21), dp(21)));
        TextView label = text("视频链接", 15, INK, true); LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1); labelParams.leftMargin = dp(7); inputHeader.addView(label, labelParams);
        pasteButton = text("粘贴", 13, GREEN, true); pasteButton.setGravity(Gravity.CENTER); pasteButton.setBackground(outline(0xFFF1F5EA, LINE, 12)); pasteButton.setContentDescription("从剪贴板粘贴视频链接");
        pasteButton.setOnClickListener(v -> paste()); inputHeader.addView(pasteButton, new LinearLayout.LayoutParams(dp(64), dp(42)));
        inputCard.addView(inputHeader); space(inputCard, 12);
        linkInput = new EditText(this); linkInput.setTextSize(14); linkInput.setTextColor(INK); linkInput.setHintTextColor(0xFF98A298);
        linkInput.setHint("粘贴 YouTube 或 Shorts 链接"); linkInput.setPadding(dp(14), dp(13), dp(14), dp(13));
        linkInput.setGravity(Gravity.TOP | Gravity.START); linkInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        linkInput.setMinLines(2); linkInput.setMaxLines(4); linkInput.setBackground(outline(BG, LINE, 13)); linkInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE);
        linkInput.setContentDescription("YouTube 视频链接");
        inputCard.addView(linkInput, new LinearLayout.LayoutParams(-1, -2)); space(inputCard, 14);
        parseButton = button("解析视频   ↗", INK, 0xFFFFFFFF); parseButton.setOnClickListener(v -> analyze()); inputCard.addView(parseButton);
        TextView hint = text("视频 / Shorts    ·    后台下载    ·    不添加水印", 10, MUTED, false); hint.setGravity(Gravity.CENTER); hint.setPadding(0, dp(13), 0, 0); inputCard.addView(hint);
        content.addView(inputCard);

        previewBox = column(); LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, -2); previewParams.topMargin = dp(16); content.addView(previewBox, previewParams);
        space(content, 25);
        LinearLayout downloadsHeader = row(); downloadsHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView downloads = text("下载清单", 18, INK, true); downloadsHeader.addView(downloads);
        countLabel = text("0", 11, GREEN, true); countLabel.setGravity(Gravity.CENTER); countLabel.setBackground(shape(0xFFE3EBCF, 8));
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(dp(25), dp(23)); countParams.leftMargin = dp(8); downloadsHeader.addView(countLabel, countParams);
        View flexible = new View(this); downloadsHeader.addView(flexible, new LinearLayout.LayoutParams(0, 1, 1));
        TextView folder = text("打开文件夹 ↗", 11, GREEN, true); folder.setGravity(Gravity.CENTER); folder.setMinHeight(dp(44)); folder.setOnClickListener(v -> openFolder()); downloadsHeader.addView(folder);
        content.addView(downloadsHeader); space(content, 10);
        jobsBox = column(); content.addView(jobsBox);
        TextView footer = text("仅保存你拥有版权或已获授权的内容\n原视频自带的水印会保留", 10, MUTED, false); footer.setGravity(Gravity.CENTER); footer.setLineSpacing(dp(5), 1); footer.setPadding(0, dp(22), 0, 0); content.addView(footer);
        linkInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (preview != null) {
                    try { if (VideoLink.normalize(s.toString()).equals(parsedUrl)) return; } catch (Exception ignored) { }
                    preview = null; parsedUrl = ""; previewBox.removeAllViews(); previewBox.setVisibility(View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        linkInput.setOnEditorActionListener((view, action, event) -> { if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { analyze(); return true; } return false; });
    }

    private void paste() {
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        if (!clipboard.hasPrimaryClip() || clipboard.getPrimaryClip() == null || clipboard.getPrimaryClip().getItemCount() == 0) { toast("剪贴板里还没有链接"); return; }
        CharSequence value = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
        if (value != null) { linkInput.setText(value); linkInput.setSelection(linkInput.length()); }
    }
    private void analyze() {
        if (parsing || updating || importing) { toast(importing ? "正在导入登录凭据，请稍候" : updating ? "引擎正在更新，请稍候" : "正在解析，请稍候"); return; }
        final String url;
        try { url = VideoLink.normalize(linkInput.getText().toString()); }
        catch (Exception e) { linkInput.setError(e.getMessage()); return; }
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(linkInput.getWindowToken(), 0);
        linkInput.clearFocus(); preview = null; parsedUrl = ""; parsing = true;
        parseButton.setText("正在解析…"); parseButton.setEnabled(false); parseButton.setAlpha(.65f); linkInput.setEnabled(false); pasteButton.setEnabled(false);
        previewBox.setVisibility(View.VISIBLE); previewBox.removeAllViews();
        LinearLayout loading = card(); LinearLayout row = row(); row.setGravity(Gravity.CENTER_VERTICAL);
        ProgressBar spinner = new ProgressBar(this); row.addView(spinner, new LinearLayout.LayoutParams(dp(26), dp(26)));
        TextView status = text("正在获取视频信息…", 14, INK, true); status.setPadding(dp(14), 0, 0, 0); row.addView(status); loading.addView(row); previewBox.addView(loading);
        previewProcess = "preview-" + UUID.randomUUID(); final String process = previewProcess;
        previewFuture = network.submit(() -> {
            JSONObject result = null; Exception error = null;
            Engine.lock.readLock().lock();
            try {
                Engine.init(this);
                if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("--dump-single-json").addOption("--skip-download").addOption("--no-playlist").addOption("--no-warnings")
                    .addOption("--socket-timeout", 15).addOption("--retries", 1).addOption("--extractor-retries", 1);
                JSONObject info = new JSONObject(Engine.execute(this, request, process, null).getOut());
                String live = info.optString("live_status");
                if (live.equals("is_live") || live.equals("is_upcoming") || live.equals("post_live")) throw new IOException("直播尚未结束，请待视频处理完成后下载");
                // Keep only display data; signed stream URLs remain transient inside the engine.
                result = new JSONObject().put("title", info.optString("title", "YouTube 视频")).put("uploader", info.optString("uploader", "YouTube"))
                    .put("duration", info.optLong("duration")).put("thumbnail", info.optString("thumbnail"));
            } catch (Exception e) { error = e; }
            finally { Engine.lock.readLock().unlock(); }
            JSONObject value = result; Exception failure = error;
            main.post(() -> {
                if (isDestroyed() || !process.equals(previewProcess)) return;
                parsing = false; previewProcess = ""; parseButton.setText("解析视频   ↗"); parseButton.setEnabled(true); parseButton.setAlpha(1);
                linkInput.setEnabled(true); pasteButton.setEnabled(true);
                if (failure == null) { preview = value; parsedUrl = url; showPreview(); }
                else showError(failure);
            });
        });
    }

    private void showError(Exception error) {
        previewBox.removeAllViews(); LinearLayout card = card();
        String message = ErrorMessages.safeDetails(error.getMessage());
        card.addView(text(ErrorMessages.needsVerification(message) ? "需要完成 YouTube 验证" : "暂时无法解析", 16, INK, true));
        TextView detail = text(message.startsWith("直播尚未") ? message : Engine.friendly(error), 13, MUTED, false); detail.setPadding(0, dp(10), 0, dp(4)); card.addView(detail);
        if (ErrorMessages.needsSession(message)) {
            TextView help = text("可以只用手机：在 Edge 完成登录 / 验证，用 Cookie-Editor 导出 Netscape 会话，再返回这里粘贴。无需电脑或 TXT 文件；验证后仍可能被 YouTube 拒绝。", 12, MUTED, false);
            help.setPadding(0, dp(9), 0, dp(12)); card.addView(help);
            TextView importCookies = button("手机登录 / 验证", LIME, INK); importCookies.setOnClickListener(v -> showMobileLogin()); card.addView(importCookies);
            LinearLayout actions = row(); action(actions, "已复制，粘贴会话", () -> pasteCookieSession()); card.addView(actions);
        }
        TextView more = text("查看错误详情", 12, GREEN, true); more.setMinHeight(dp(44)); more.setGravity(Gravity.CENTER_VERTICAL); more.setOnClickListener(v -> errorDetails(message)); card.addView(more);
        previewBox.addView(card);
    }
    private void showPreview() {
        if (preview == null) return;
        JSONObject info = preview;
        previewBox.setVisibility(View.VISIBLE); previewBox.removeAllViews();
        LinearLayout card = card();
        FrameLayout imageFrame = new FrameLayout(this); imageFrame.setBackground(shape(0xFFE6ECDF, 14)); imageFrame.setClipToOutline(true);
        ImageView thumbnail = new ImageView(this); thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP); imageFrame.addView(thumbnail, new FrameLayout.LayoutParams(-1, -1));
        Icon play = new Icon("play", 0xFFFFFFFF); FrameLayout.LayoutParams playParams = new FrameLayout.LayoutParams(dp(44), dp(44), Gravity.CENTER); play.setBackground(shape(0x88000000, 24)); imageFrame.addView(play, playParams);
        TextView duration = text(duration(info.optLong("duration")), 11, 0xFFFFFFFF, true); duration.setPadding(dp(7), dp(4), dp(7), dp(4)); duration.setBackground(shape(0xAA10251C, 6));
        FrameLayout.LayoutParams durationParams = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END); durationParams.setMargins(0, 0, dp(9), dp(9)); imageFrame.addView(duration, durationParams);
        card.addView(imageFrame, new LinearLayout.LayoutParams(-1, dp(164))); space(card, 14);
        TextView title = text(info.optString("title"), 17, INK, true); title.setMaxLines(3); title.setEllipsize(TextUtils.TruncateAt.END); card.addView(title);
        TextView author = text(info.optString("uploader") + "  ·  MP4 视频", 12, MUTED, false); author.setPadding(0, dp(7), 0, dp(18)); card.addView(author);
        card.addView(text("画质上限", 12, MUTED, true)); space(card, 10);
        LinearLayout qualities = row(); int[] choices = {0, 1080, 720, 480};
        for (int choice : choices) {
            TextView chip = text(choice == 0 ? "原画" : choice + "p", 12, height == choice ? 0xFFFFFFFF : INK, true); chip.setGravity(Gravity.CENTER);
            chip.setBackground(height == choice ? shape(GREEN, 11) : outline(BG, LINE, 11)); chip.setContentDescription(choice == 0 ? "选择最佳可用画质" : "选择最高 " + choice + "p");
            chip.setSelected(height == choice);
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(0, dp(44), 1); if (choice != 0) chipParams.leftMargin = dp(7); qualities.addView(chip, chipParams);
            chip.setOnClickListener(v -> { height = choice; showPreview(); });
        }
        card.addView(qualities); space(card, 8);
        TextView note = text("自动选择上限内的最佳画质，保留原始音轨。", 10, MUTED, false); note.setPadding(0, 0, 0, dp(15)); card.addView(note);
        TextView download = button("下载到手机   ↓", LIME, INK);
        download.setOnClickListener(v -> {
            if (updating || importing) { toast("正在更新或导入，请稍候"); return; }
            Job job = new Job(UUID.randomUUID().toString(), parsedUrl, info.optString("title", "YouTube 视频"), height);
            requestDownload(job);
        });
        card.addView(download); previewBox.addView(card);
        String thumbnailUrl = info.optString("thumbnail");
        if (thumbnailUrl.equals(cachedThumbnailUrl) && cachedThumbnail != null) { thumbnail.setImageBitmap(cachedThumbnail); return; }
        if (thumbnailUrl.startsWith("https://")) network.execute(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection)new URL(thumbnailUrl).openConnection();
                connection.setConnectTimeout(8000); connection.setReadTimeout(8000);
                Bitmap bitmap;
                try (InputStream in = connection.getInputStream()) { bitmap = BitmapFactory.decodeStream(in); } finally { connection.disconnect(); }
                main.post(() -> { if (!isDestroyed()) { cachedThumbnailUrl = thumbnailUrl; cachedThumbnail = bitmap; if (thumbnail.isAttachedToWindow()) thumbnail.setImageBitmap(bitmap); } });
            } catch (Exception ignored) { }
        });
    }

    private void requestDownload(Job job) {
        if (importing) { toast("正在导入登录凭据，请稍候"); return; }
        if (DownloadService.isExecuting(job.id)) { toast("任务正在结束，请稍候再试"); return; }
        for (Job active : PocketApp.instance.jobs.all()) {
            if (!active.id.equals(job.id) && active.url.equals(job.url) && active.height == job.height && (DownloadPolicy.cancellable(active.state) || active.state.equals("saving"))) { toast("相同视频已在下载清单中"); return; }
        }
        pendingJob = job;
        if (Build.VERSION.SDK_INT <= 28 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 71); return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !getPreferences(MODE_PRIVATE).getBoolean("notificationAsked", false)) {
            getPreferences(MODE_PRIVATE).edit().putBoolean("notificationAsked", true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 72); return;
        }
        enqueuePending();
    }
    private void enqueuePending() {
        if (pendingJob == null) return;
        Job job = pendingJob; pendingJob = null;
        Job existing = PocketApp.instance.jobs.get(job.id);
        if (existing == null) PocketApp.instance.jobs.add(job);
        else if (DownloadPolicy.retryable(existing.state)) PocketApp.instance.jobs.edit(job.id, j -> { j.state = "queued"; j.detail = "等待下载"; j.progress = 0; j.rawError = ""; });
        else return;
        try {
            startForegroundService(new Intent(this, DownloadService.class).setAction(DownloadService.ENQUEUE).putExtra("id", job.id));
            toast("已加入下载清单");
        } catch (Exception error) {
            PocketApp.instance.jobs.edit(job.id, j -> { j.state = "failed"; j.detail = "无法启动后台下载，请保持应用在前台后重试"; j.rawError = String.valueOf(error.getMessage()); });
        }
        renderJobs();
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == 71) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) enqueuePending();
            else { pendingJob = null; toast("需要存储权限才能保存到系统下载文件夹"); }
        } else if (requestCode == 72) enqueuePending();
    }

    private void renderJobs() {
        java.util.List<Job> jobs = PocketApp.instance.jobs.all();
        StringBuilder signature = new StringBuilder(); for (Job job : jobs) signature.append(job.id).append(job.state).append(job.progress).append(job.detail);
        if (jobsSignature.equals(signature.toString()) && jobsBox.getChildCount() > 0) return;
        jobsSignature = signature.toString(); jobsBox.removeAllViews(); countLabel.setText(String.valueOf(jobs.size()));
        if (jobs.isEmpty()) {
            LinearLayout empty = card(); empty.setGravity(Gravity.CENTER); empty.setPadding(dp(20), dp(25), dp(20), dp(25));
            Icon icon = new Icon("download", GREEN); icon.setBackground(shape(0xFFEBF0DF, 18)); empty.addView(icon, new LinearLayout.LayoutParams(dp(52), dp(52))); space(empty, 13);
            empty.addView(text("收藏，从一个链接开始", 14, INK, true)); space(empty, 7); empty.addView(text("下载后，即可随时离线观看", 11, MUTED, false)); jobsBox.addView(empty); return;
        }
        for (Job job : jobs) {
            LinearLayout card = card(); LinearLayout top = row(); top.setGravity(Gravity.CENTER_VERTICAL);
            boolean completed = job.state.equals("completed"); boolean failed = DownloadPolicy.retryable(job.state);
            Icon icon = new Icon(completed ? "check" : failed ? "retry" : "download", completed ? GREEN : failed ? 0xFF9A7040 : GREEN);
            icon.setBackground(shape(completed ? 0xFFE5EEDB : failed ? 0xFFF5EDDF : BG, 13)); top.addView(icon, new LinearLayout.LayoutParams(dp(40), dp(44)));
            LinearLayout information = column(); LinearLayout.LayoutParams informationParams = new LinearLayout.LayoutParams(0, -2, 1); informationParams.leftMargin = dp(11); top.addView(information, informationParams);
            TextView title = text(job.title, 13, INK, true); title.setMaxLines(2); title.setEllipsize(TextUtils.TruncateAt.END); information.addView(title);
            TextView meta = text(DownloadPolicy.quality(job.height) + "  ·  " + stateLabel(job.state), 10, MUTED, false); meta.setPadding(0, dp(5), 0, 0); information.addView(meta);
            card.addView(top); space(card, 12);
            TextView detail = text(job.detail, 11, failed ? 0xFF956431 : MUTED, false); detail.setMaxLines(3); card.addView(detail);
            if (job.state.equals("running") || job.state.equals("saving")) {
                ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal); progress.setMax(100); progress.setProgress(job.progress); progress.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN)); progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(LINE));
                LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(5)); progressParams.topMargin = dp(12); card.addView(progress, progressParams);
            }
            LinearLayout actions = row(); actions.setGravity(Gravity.END); space(card, 4);
            if (completed) {
                action(actions, "分享", () -> share(job)); action(actions, "播放  ↗", () -> play(job));
            } else if (DownloadPolicy.cancellable(job.state)) {
                action(actions, "取消下载", () -> { PocketApp.instance.cancel(job.id); renderJobs(); });
            } else if (failed) {
                if (!job.rawError.isEmpty()) action(actions, "详情", () -> errorDetails(job.rawError));
                if (ErrorMessages.needsSession(job.rawError)) action(actions, "登录 / 验证", () -> settings());
                action(actions, "重新下载 ↻", () -> requestDownload(job));
            }
            if (actions.getChildCount() > 0) card.addView(actions);
            LinearLayout.LayoutParams jobParams = new LinearLayout.LayoutParams(-1, -2); jobParams.bottomMargin = dp(10); jobsBox.addView(card, jobParams);
        }
    }
    private void action(LinearLayout row, String label, Runnable action) {
        TextView button = text(label, 12, GREEN, true); button.setGravity(Gravity.CENTER); button.setPadding(dp(12), 0, dp(6), 0); button.setMinHeight(dp(44)); button.setOnClickListener(v -> action.run()); row.addView(button);
    }
    private void play(Job job) {
        try {
            Uri uri = Uri.parse(job.uri);
            try (android.os.ParcelFileDescriptor ignored = getContentResolver().openFileDescriptor(uri, "r")) { if (ignored == null) throw new IOException(); }
            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (ActivityNotFoundException e) { toast("请安装支持 MP4 的视频播放器"); }
        catch (Exception e) { toast("文件已被移动或删除，请重新下载"); }
    }
    private void share(Job job) {
        try {
            Uri uri = Uri.parse(job.uri);
            try (android.os.ParcelFileDescriptor ignored = getContentResolver().openFileDescriptor(uri, "r")) { if (ignored == null) throw new IOException(); }
            Intent send = new Intent(Intent.ACTION_SEND).setType("video/mp4").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            send.setClipData(ClipData.newRawUri("视频", uri)); startActivity(Intent.createChooser(send, "分享视频"));
        } catch (Exception e) { toast("无法分享，请确认视频文件仍在手机中"); }
    }
    private void openFolder() {
        try { startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)); }
        catch (Exception e) { toast("请在文件管理器中打开 Download/TubePocket"); }
    }

    private void settings() {
        String version = YoutubeDL.getInstance().version(this); if (version == null) version = BuildConfig.BUNDLED_ENGINE;
        LinearLayout body = column(); body.setPadding(dp(22), dp(12), dp(22), dp(12));
        body.addView(text("版本 " + BuildConfig.VERSION_NAME + "  ·  保存到 Download/TubePocket", 12, MUTED, false)); space(body, 22);
        body.addView(text("YouTube 登录凭据", 17, INK, true)); space(body, 9);
        CookieStore store = new CookieStore(getNoBackupFilesDir());
        body.addView(text(store.hasSession() ? "已导入 · 仅在本机保存" : "尚未导入", 12, GREEN, true)); space(body, 12);
        body.addView(text("只用手机 Edge 即可操作：在 Edge 的“扩展”中安装 Cookie-Editor，登录 YouTube 并完成页面验证；回到视频页，在扩展里点 Export → Netscape，再返回拾光粘贴会话。\n\n登录凭据只保存在本机。过期后需重新复制；YouTube 仍可能拒绝请求。", 12, MUTED, false)); space(body, 14);
        TextView mobileButton = button("手机登录 / 验证", LIME, INK); body.addView(mobileButton); space(body, 9);
        TextView pasteSession = button("粘贴手机浏览器会话", 0xFFECEFE6, INK); body.addView(pasteSession); space(body, 9);
        TextView importButton = button("从 cookies.txt 文件导入（可选）", 0xFFECEFE6, INK); body.addView(importButton); space(body, 9);
        TextView clearButton = button("清除已导入的凭据", 0xFFECEFE6, INK); body.addView(clearButton); space(body, 9);
        TextView verifyButton = button("用 Edge 打开 YouTube ↗", 0xFFECEFE6, INK); body.addView(verifyButton); space(body, 22);
        body.addView(text("解析引擎 · yt-dlp " + version, 13, INK, true)); space(body, 12);
        TextView updateButton = button(updating ? "更新中…" : "更新解析引擎", INK, 0xFFFFFFFF); body.addView(updateButton); space(body, 20);
        body.addView(text("隐私与使用\n会话仅在你点粘贴时读取，成功导入后清除剪贴板中的同一份会话。登录凭据保存在本应用私有目录，不进入下载文件夹或系统备份，仅供 YouTube 解析和下载使用。不要把会话发给他人。清除仅移除应用保存的副本。\n\n链接与下载记录保存在本机，无自建服务器、广告或统计 SDK。更新引擎连接 GitHub。手机网络需能访问 YouTube。应用不添加水印，原视频自带的水印会保留。仅保存你有权下载的内容。", 11, MUTED, false));
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("拾光 · 设置").setView(scroll)
            .setNeutralButton("开源许可", (d, w) -> licenses()).setPositiveButton("关闭", null).create();
        importButton.setOnClickListener(v -> { dialog.dismiss(); pickCookieFile(); });
        mobileButton.setOnClickListener(v -> { dialog.dismiss(); showMobileLogin(); });
        pasteSession.setOnClickListener(v -> { dialog.dismiss(); pasteCookieSession(); });
        clearButton.setOnClickListener(v -> {
            if (credentialsBusy()) return;
            try { store.clear(); dialog.dismiss(); toast("已清除本应用保存的登录凭据"); settings(); }
            catch (IOException e) { toast(e.getMessage()); }
        });
        verifyButton.setOnClickListener(v -> openInEdge(currentVideoUrl()));
        updateButton.setOnClickListener(v -> { dialog.dismiss(); updateEngine(); });
        dialog.show();
    }
    private void showMobileLogin() {
        LinearLayout body = column(); body.setPadding(dp(22), dp(12), dp(22), dp(16));
        body.addView(text("全程只用手机 Edge，无需电脑或 TXT 文件。\n登录和验证码由你在 Edge 中完成。", 13, INK, true)); space(body, 18);
        body.addView(text("1. 确认 Edge 菜单里有“扩展”入口", 14, INK, true)); space(body, 9);
        body.addView(text("已有扩展入口可直接进行下一步。没有时先更新 Edge；能否安装扩展以 Edge 页面显示为准。", 12, MUTED, false)); space(body, 9);
        TextView updateEdge = button("Edge 官方更新页（如需）↗", 0xFFECEFE6, INK); body.addView(updateEdge); space(body, 18);
        body.addView(text("2. 在 Edge 安装 Cookie-Editor 扩展", 14, INK, true)); space(body, 9);
        TextView addon = button("在 Edge 打开官方扩展页 ↗", 0xFFECEFE6, INK); body.addView(addon); space(body, 18);
        body.addView(text("3. 用 Edge 打开视频并登录 / 验证", 14, INK, true)); space(body, 9);
        TextView video = button("用 Edge 打开当前视频 ↗", INK, 0xFFFFFFFF); body.addView(video); space(body, 18);
        body.addView(text("4. 回到 YouTube 视频页，在 Edge 菜单 → 扩展 / 扩展程序 → Cookie-Editor 中，点 Export，选择 Netscape。扩展会将会话复制到剪贴板。", 13, MUTED, false)); space(body, 18);
        body.addView(text("5. 返回拾光，粘贴并重新解析", 14, INK, true)); space(body, 9);
        TextView paste = button("粘贴会话并保存到手机", LIME, INK); body.addView(paste); space(body, 14);
        body.addView(text("Cookie-Editor 是第三方扩展，由你在 Edge 中安装并授权。会话含登录凭据，不要发到聊天或交给他人。导入后清除剪贴板中的这份会话；本机保存的副本可在设置中清除。YouTube 仍可能拒绝请求。", 11, MUTED, false));
        ScrollView scroll = new ScrollView(this); scroll.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("手机登录 / 验证").setView(scroll).setPositiveButton("关闭", null).create();
        updateEdge.setOnClickListener(v -> openExternal(EDGE_ANDROID));
        addon.setOnClickListener(v -> openInEdge(COOKIE_EDITOR));
        video.setOnClickListener(v -> openInEdge(currentVideoUrl()));
        paste.setOnClickListener(v -> { dialog.dismiss(); pasteCookieSession(); });
        dialog.show();
    }
    private void openInEdge(String url) {
        for (String browser : new String[]{"com.microsoft.emmx", "com.microsoft.emmx.beta", "com.microsoft.emmx.dev", "com.microsoft.emmx.canary"}) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE).setPackage(browser)); return; }
            catch (ActivityNotFoundException | SecurityException ignored) { }
        }
        toast("请先启用支持扩展的 Edge 安卓版"); openExternal(EDGE_ANDROID);
    }
    private void openExternal(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)); }
        catch (ActivityNotFoundException | SecurityException e) { toast("请用手机浏览器打开官方页面"); }
    }
    private String currentVideoUrl() {
        try { return VideoLink.normalize(linkInput.getText().toString()); } catch (Exception ignored) { return "https://www.youtube.com/"; }
    }
    private void pasteCookieSession() {
        if (credentialsBusy()) return;
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        ClipData clip;
        try { clip = clipboard.getPrimaryClip(); } catch (SecurityException e) { toast("请允许本次粘贴后重试"); return; }
        if (clip == null || clip.getItemCount() == 0 || clip.getItemAt(0).getText() == null) { toast("请先在 Cookie-Editor 中导出 Netscape 会话"); return; }
        String content = clip.getItemAt(0).getText().toString();
        if (content.length() > CookieFile.MAX_BYTES) { toast("会话内容不能超过 1 MB"); return; }
        importCookies(() -> new ByteArrayInputStream(content.getBytes(java.nio.charset.StandardCharsets.UTF_8)), () -> {
            try {
                ClipData current = clipboard.getPrimaryClip();
                if (current != null && current.getItemCount() > 0 && TextUtils.equals(current.getItemAt(0).getText(), content)) {
                    if (Build.VERSION.SDK_INT >= 28) clipboard.clearPrimaryClip();
                    else clipboard.setPrimaryClip(ClipData.newPlainText("", ""));
                }
            } catch (SecurityException ignored) { }
        });
    }
    private boolean credentialsBusy() {
        if (parsing || updating || importing || PocketApp.instance.jobs.hasActive()) { toast("请先完成或取消当前解析、下载或导入任务"); return true; }
        return false;
    }
    private void pickCookieFile() {
        if (credentialsBusy()) return;
        Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(pick, 81); }
        catch (ActivityNotFoundException e) { toast("请启用系统文件选择器后重试"); }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 81 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (credentialsBusy()) return;
        Uri uri = data.getData(); importCookies(() -> getContentResolver().openInputStream(uri), null);
    }
    private void importCookies(Callable<InputStream> source, Runnable afterSuccess) {
        importing = true; parseButton.setEnabled(false); parseButton.setAlpha(.65f);
        network.execute(() -> {
            String message; boolean success = false;
            try (InputStream in = source.call()) {
                if (in == null) throw new IOException("无法读取文件，请重新选择");
                int count = new CookieStore(getNoBackupFilesDir()).importFrom(in);
                message = "已保存 " + count + " 条 YouTube 会话记录，请重新解析视频"; success = true;
            } catch (IllegalArgumentException e) { message = e.getMessage(); }
            catch (Exception e) { message = "保存失败，请确认会话为 Netscape 格式且不超过 1 MB"; }
            String result = message; boolean saved = success;
            main.post(() -> { importing = false; if (saved && afterSuccess != null) afterSuccess.run(); if (!isDestroyed()) { parseButton.setEnabled(true); parseButton.setAlpha(1); toast(result); } });
        });
    }
    private void updateEngine() {
        if (updating || parsing || importing || PocketApp.instance.jobs.hasActive()) { toast("请等待当前解析、导入或下载完成后再更新"); return; }
        updating = true; parseButton.setEnabled(false); parseButton.setAlpha(.65f); toast("正在检查并更新解析引擎…");
        network.execute(() -> {
            String message;
            Engine.lock.writeLock().lock();
            try {
                Engine.init(this);
                YoutubeDL.UpdateStatus status = YoutubeDL.getInstance().updateYoutubeDL(this, YoutubeDL.UpdateChannel._STABLE);
                message = status == YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE ? "解析引擎已是最新版本" : "解析引擎已更新";
            } catch (Exception e) { message = "更新失败，请检查手机能否访问 GitHub 后重试"; }
            finally { Engine.lock.writeLock().unlock(); }
            String result = message; main.post(() -> { updating = false; if (!isDestroyed()) { parseButton.setEnabled(true); parseButton.setAlpha(1); toast(result); } });
        });
    }
    private void licenses() {
        new AlertDialog.Builder(this).setTitle("开源许可")
            .setMessage("拾光 / TubePocket " + BuildConfig.VERSION_NAME + "\nGPL-3.0-or-later\n\nyoutubedl-android 0.18.1 · GPL-3.0\nyt-dlp · Unlicense（部分打包组件使用其他开源许可）\nFFmpeg · LGPL/GPL，依构建配置\nAndroidX · Apache-2.0\nKotlin · Apache-2.0\nJackson · Apache-2.0\nCommons IO · Apache-2.0\n\n完整许可、构建说明和对应源码见本项目源码包中的 LICENSE、THIRD_PARTY_NOTICES.md。")
            .setPositiveButton("查看许可文本", (d, w) -> {
                try (InputStream in = getAssets().open("OPEN_SOURCE_LICENSES.txt")) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n; while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                    errorDetails(out.toString("UTF-8"));
                } catch (IOException e) { toast("请查看源码包中的许可文件"); }
            }).setNegativeButton("关闭", null).show();
    }
    private void errorDetails(String message) {
        TextView text = text(message, 12, INK, false); text.setTextIsSelectable(true); text.setPadding(dp(22), dp(16), dp(22), dp(16));
        ScrollView scroll = new ScrollView(this); scroll.addView(text);
        new AlertDialog.Builder(this).setTitle("详细信息").setView(scroll).setPositiveButton("关闭", null).show();
    }
    private void receiveShare(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            String shared = intent.getStringExtra(Intent.EXTRA_TEXT); if (shared != null) { linkInput.setText(shared); toast("已接收链接，点击解析视频"); }
        }
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); if (!parsing) receiveShare(intent); else toast("正在解析当前视频，请稍后粘贴新链接"); }
    @Override protected void onResume() { super.onResume(); main.removeCallbacks(refresh); refresh.run(); }
    @Override protected void onPause() { main.removeCallbacks(refresh); super.onPause(); }
    @Override public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putString("link", linkInput.getText().toString()); state.putInt("height", height); state.putString("parsedUrl", parsedUrl);
        if (preview != null) state.putString("preview", preview.toString()); if (pendingJob != null) state.putString("pendingJob", pendingJob.json().toString());
    }
    @Override protected void onDestroy() {
        if (previewFuture != null) previewFuture.cancel(true);
        if (!previewProcess.isEmpty()) Engine.cancel(previewProcess);
        main.removeCallbacks(refresh); network.shutdown(); super.onDestroy();
    }

    private LinearLayout column() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); return layout; }
    private LinearLayout row() { LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.HORIZONTAL); return layout; }
    private LinearLayout card() { LinearLayout layout = column(); layout.setBackground(outline(0xFFFFFFFF, LINE, 20)); layout.setPadding(dp(18), dp(16), dp(18), dp(16)); return layout; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this); text.setText(value); text.setTextSize(size); text.setTextColor(color); text.setIncludeFontPadding(false);
        text.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL)); return text;
    }
    private TextView button(String label, int background, int foreground) {
        TextView button = text(label, 15, foreground, true); button.setGravity(Gravity.CENTER); button.setBackground(shape(background, 13));
        button.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(52))); button.setClickable(true); button.setFocusable(true); return button;
    }
    private GradientDrawable shape(int color, int radius) { GradientDrawable background = new GradientDrawable(); background.setColor(color); background.setCornerRadius(dp(radius)); return background; }
    private GradientDrawable outline(int color, int stroke, int radius) { GradientDrawable background = shape(color, radius); background.setStroke(dp(1), stroke); return background; }
    private void space(LinearLayout layout, int height) { layout.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height))); }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_SHORT).show(); }
    private static String duration(long seconds) { return seconds >= 3600 ? String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60) : String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60); }
    private static String stateLabel(String state) {
        switch (state) { case "queued": return "排队中"; case "running": return "下载中"; case "saving": return "保存中"; case "completed": return "已完成"; case "cancelled": return "已取消"; case "interrupted": return "已中断"; default: return "下载失败"; }
    }

    private final class Icon extends View {
        private final String type; private final int color; private final Paint paint = new Paint(3); private final Path path = new Path();
        Icon(String type, int color) { super(MainActivity.this); this.type = type; this.color = color; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); canvas.save(); float side = Math.min(getWidth(), getHeight()); canvas.translate((getWidth() - side) / 2, (getHeight() - side) / 2); canvas.scale(side / 48, side / 48);
            paint.setColor(color); paint.setStrokeWidth(2); paint.setStyle(Paint.Style.STROKE); paint.setStrokeCap(Paint.Cap.ROUND); paint.setStrokeJoin(Paint.Join.ROUND);
            path.reset();
            if (type.equals("download") || type.equals("hero")) {
                if (type.equals("hero")) { paint.setAlpha(35); canvas.drawCircle(24, 24, 23, paint); canvas.drawCircle(24, 24, 17, paint); paint.setAlpha(255); }
                path.moveTo(24, 12); path.lineTo(24, 29); path.moveTo(17, 23); path.lineTo(24, 30); path.lineTo(31, 23); path.moveTo(14, 32); path.lineTo(14, 36); path.lineTo(34, 36); path.lineTo(34, 32); canvas.drawPath(path, paint);
            } else if (type.equals("settings")) {
                canvas.drawCircle(24, 24, 5, paint); canvas.drawCircle(24, 24, 11, paint);
                for (int i = 0; i < 8; i++) { canvas.save(); canvas.rotate(i * 45, 24, 24); canvas.drawLine(24, 10, 24, 13, paint); canvas.restore(); }
            } else if (type.equals("link")) {
                canvas.save(); canvas.rotate(-38, 24, 24); canvas.drawRoundRect(9, 18, 27, 30, 6, 6, paint); canvas.drawRoundRect(21, 18, 39, 30, 6, 6, paint); canvas.restore();
            } else if (type.equals("play")) {
                paint.setStyle(Paint.Style.FILL); path.moveTo(20, 15); path.lineTo(33, 24); path.lineTo(20, 33); path.close(); canvas.drawPath(path, paint);
            } else if (type.equals("check")) {
                path.moveTo(14, 24); path.lineTo(21, 31); path.lineTo(34, 17); canvas.drawPath(path, paint);
            } else if (type.equals("retry")) {
                canvas.drawArc(13, 13, 35, 35, 35, 290, false, paint); path.moveTo(35, 13); path.lineTo(35, 22); path.lineTo(27, 22); canvas.drawPath(path, paint);
            }
            canvas.restore();
        }
    }
}
