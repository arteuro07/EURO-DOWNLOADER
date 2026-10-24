package com.euro.downloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.database.Cursor;
import android.os.Build;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.graphics.Typeface;
import android.graphics.Color;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.concurrent.Future;
import dev.ffmpegkit_maintained.ytdlp.YtDlp;
import dev.ffmpegkit_maintained.ytdlp.YtDlpRequest;
import dev.ffmpegkit_maintained.ytdlp.YtDlpResponse;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class MainActivity extends Activity {
    private EditText urlInput;
    private TextView status;
    private Button downloadButton;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(232, 239, 250));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void space(LinearLayout root, int height) {
        View v = new View(this);
        root.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(26), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(10, 18, 34));
        scroll.addView(root);

        TextView brand = text("EURO", 16, true);
        brand.setTextColor(Color.rgb(38, 210, 170));
        root.addView(brand);
        space(root, 8);
        root.addView(text("DOWNLOADER Max", 28, true));
        space(root, 5);
        root.addView(text("VIDEO • AUDIO • MULTI-PLATEFORME", 12, true));
        space(root, 8);
        root.addView(text("Colle un lien de vidéo depuis YouTube, TikTok, Facebook, Instagram et de nombreux autres sites. Le moteur tente d’extraire la vidéo ; les fichiers directs restent pris en charge.", 15, false));
        space(root, 24);
        root.addView(text("Lien de la vidéo ou fichier direct", 16, true));
        space(root, 8);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(15);
        urlInput.setHint("https://site.com/video.mp4");
        urlInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        urlInput.setTextColor(Color.rgb(20, 30, 45));
        urlInput.setHintTextColor(Color.rgb(110, 120, 140));
        urlInput.setBackgroundColor(Color.WHITE);
        root.addView(urlInput, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58)));
        space(root, 8);
        Button pasteButton = new Button(this);
        pasteButton.setText("📋  COLLER UN LIEN");
        root.addView(pasteButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(46)));
        pasteButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip().getItemCount() > 0) {
                CharSequence clip = clipboard.getPrimaryClip().getItemAt(0).coerceToText(this);
                if (clip != null) { urlInput.setText(clip.toString().trim()); status.setText("Lien collé. Appuie sur Analyser et télécharger."); }
            } else { Toast.makeText(this, "Aucun lien copié dans le presse-papiers", Toast.LENGTH_SHORT).show(); }
        });

        space(root, 14);
        downloadButton = new Button(this);
        downloadButton.setText("⬇  ANALYSER ET TÉLÉCHARGER");
        root.addView(downloadButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));
        space(root, 24);
        root.addView(text("État", 17, true));
        space(root, 8);
        status = text("Prêt. Colle un lien de vidéo public ou une URL directe.", 14, false);
        status.setPadding(dp(14), dp(14), dp(14), dp(14));
        status.setTextColor(Color.rgb(35, 48, 66));
        status.setBackgroundColor(Color.rgb(232, 239, 248));
        root.addView(status, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        space(root, 24);
        root.addView(text("À savoir", 17, true));
        space(root, 8);
        root.addView(text("• Les liens directs .mp4, .webm et autres fichiers sont vérifiés avant téléchargement.\n• Si le serveur renvoie une page HTML, le téléchargement est refusé.\n• Les liens de pages TikTok, Instagram, Facebook ou YouTube ne sont pas forcément des liens vidéo directs. L’extraction de ces pages nécessite un module spécifique et peut être limitée par les règles de la plateforme.\n• Les fichiers sont enregistrés dans Download.", 14, false));

        downloadButton.setOnClickListener(v -> startDownload());
        setContentView(scroll);
    }

    private void startDownload() {
        final String raw = urlInput.getText().toString().trim();
        Uri uri = Uri.parse(raw);
        String scheme = uri.getScheme();
        if (raw.isEmpty() || scheme == null ||
                !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http")) ||
                uri.getHost() == null) {
            status.setText("Lien invalide. Utilise une URL HTTP ou HTTPS complète.");
            return;
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);

        downloadButton.setEnabled(false);
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.equals("tiktok.com") || host.endsWith(".tiktok.com") ||
                host.equals("youtube.com") || host.endsWith(".youtube.com") ||
                host.equals("youtu.be") || host.equals("instagram.com") || host.endsWith(".instagram.com") ||
                host.equals("facebook.com") || host.endsWith(".facebook.com") ||
                host.equals("fb.watch") || host.equals("vm.tiktok.com") || host.equals("vt.tiktok.com")) {
            startExtractor(raw);
            return;
        }
        status.setText("Vérification du lien et du type de contenu…");

        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                URL checkUrl = new URL(raw);
                connection = (HttpURLConnection) checkUrl.openConnection();
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                connection.setRequestProperty("Range", "bytes=0-0");
                connection.setRequestProperty("User-Agent", "EURO-DOWNLOADER-Max/1.1 (Android)");
                connection.connect();

                int code = connection.getResponseCode();
                String contentType = connection.getContentType();
                String disposition = connection.getHeaderField("Content-Disposition");
                String finalUrl = connection.getURL().toString();
                if (code < 200 || code >= 400) {
                    throw new IllegalStateException("Le serveur a répondu HTTP " + code + ".");
                }

                String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
                String finalHost = Uri.parse(finalUrl).getHost();
                if (type.contains("text/html") || type.contains("application/xhtml") || type.isEmpty()) {
                    if (connection != null) connection.disconnect();
                    startExtractor(raw);
                    return;
                }

                String fileName = fileNameFrom(raw, disposition);
                if (fileName == null || fileName.trim().isEmpty()) {
                    String path = Uri.parse(finalUrl).getLastPathSegment();
                    fileName = (path == null || path.trim().isEmpty()) ? "euro_download_" + System.currentTimeMillis() + extensionFor(type) : path;
                    try { fileName = URLDecoder.decode(fileName, StandardCharsets.UTF_8.name()); } catch (Exception ignored) {}
                }
                fileName = fileName.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
                if (fileName.length() > 120) fileName = fileName.substring(0, 120);
                if (!fileName.contains(".")) fileName += extensionFor(type);
                final String safeName = System.currentTimeMillis() + "_" + fileName;
                final String mimeType = type;
                runOnUiThread(() -> enqueueDownload(finalUrl, safeName, mimeType));
            } catch (Exception e) {
                String message = e.getMessage() == null ? "Vérification impossible." : e.getMessage();
                runOnUiThread(() -> {
                    status.setText("Téléchargement non lancé : " + message + "\nLe serveur n’a pas permis de vérifier le fichier. Essaie un lien direct public en HTTPS.");
                    downloadButton.setEnabled(true);
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        }).start();
    }


    private void startExtractor(String videoUrl) {
        runOnUiThread(() -> status.setText("Lien de page détecté. Initialisation du moteur vidéo…"));
        new Thread(() -> {
            File outputDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (outputDir == null) outputDir = getFilesDir();
            if (!outputDir.exists()) outputDir.mkdirs();
            final File targetDir = outputDir;
            final long startTime = System.currentTimeMillis();
            try {
                YtDlp.init(getApplicationContext());
                String template = new File(targetDir, "%(title).180B.%(ext)s").getAbsolutePath();
                YtDlpRequest request = new YtDlpRequest(videoUrl)
                        .setOutputTemplate(template)
                        .addOption("-f", "best[height<=1080]/best")
                        .addOption("--no-playlist")
                        .addOption("--no-warnings")
                        .addOption("--no-write-info-json")
                        .addOption("--no-write-thumbnail")
                        .addOption("--newline");
                runOnUiThread(() -> status.setText("Analyse de la page… recherche du flux vidéo."));
                Future<YtDlpResponse> task = YtDlp.executeAsync(request, (progress, eta, line) -> {
                    final String detail = (line == null || line.trim().isEmpty()) ? "" : "\n" + line;
                    runOnUiThread(() -> status.setText("TÉLÉCHARGEMENT : " + Math.round(progress) + "%\n" +
                            (eta > 0 ? "Temps restant estimé : " + eta + " s" : "Extraction en cours") + detail));
                });
                YtDlpResponse response = task.get();
                if (response == null || !response.isSuccess()) {
                    String details = "";
                    if (response != null) {
                        String err = response.getErrorOutput();
                        String out = response.getOutput();
                        if (err != null && !err.trim().isEmpty()) details = err.trim();
                        else if (out != null && !out.trim().isEmpty()) details = out.trim();
                        if (details.length() > 900) details = details.substring(details.length() - 900);
                    }
                    throw new IllegalStateException("Le moteur n’a pas pu extraire cette vidéo." +
                            (details.isEmpty() ? " Vérifie que le lien est public et réessaie." : "\nDétail technique :\n" + details));
                }
                File[] files = targetDir.listFiles();
                File newest = null;
                if (files != null) for (File file : files) {
                    if (file.isFile() && !file.getName().endsWith(".part") &&
                            file.lastModified() >= startTime - 1000 &&
                            (newest == null || file.lastModified() > newest.lastModified())) newest = file;
                }
                if (newest == null || newest.length() == 0) throw new IllegalStateException("Aucun fichier vidéo final n’a été trouvé.");
                final File downloaded = newest;
                copyToPublicDownloads(downloaded);
                runOnUiThread(() -> {
                    status.setText("✓ TÉLÉCHARGEMENT TERMINÉ\n\nFichier : Download/" + downloaded.getName());
                    downloadButton.setEnabled(true);
                    Toast.makeText(this, "Vidéo enregistrée dans Download", Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                String msg = e.getMessage();
                if (msg == null || msg.trim().isEmpty()) msg = e.getClass().getSimpleName();
                final String error = msg;
                runOnUiThread(() -> {
                    status.setText("ÉCHEC DE L’EXTRACTION\n" + error + "\nEssaie un lien public accessible. Certaines plateformes imposent des restrictions.");
                    downloadButton.setEnabled(true);
                });
            }
        }).start();
    }

    private void copyToPublicDownloads(File source) throws Exception {
        String lowerName = source.getName().toLowerCase(Locale.ROOT);
        String mime = lowerName.endsWith(".mp4") ? "video/mp4" :
                lowerName.endsWith(".webm") ? "video/webm" :
                lowerName.endsWith(".mkv") ? "video/x-matroska" :
                lowerName.endsWith(".mp3") ? "audio/mpeg" : "application/octet-stream";
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, source.getName());
        values.put(MediaStore.Downloads.MIME_TYPE, mime);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        if (Build.VERSION.SDK_INT >= 29) values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri destination = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (destination == null) throw new IllegalStateException("Impossible de créer le fichier dans Download.");
        try (FileInputStream in = new FileInputStream(source);
             OutputStream out = getContentResolver().openOutputStream(destination)) {
            if (out == null) throw new IllegalStateException("Impossible d’écrire dans Download.");
            byte[] buffer = new byte[65536];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            out.flush();
        } catch (Exception e) {
            getContentResolver().delete(destination, null, null);
            throw e;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(destination, ready, null, null);
        }
    }

    private String fileNameFrom(String raw, String disposition) {
        if (disposition != null) {
            String lower = disposition.toLowerCase(Locale.ROOT);
            int index = lower.indexOf("filename=");
            if (index >= 0) {
                String value = disposition.substring(index + 9).trim();
                if (value.startsWith("\"")) {
                    int end = value.indexOf('"', 1);
                    if (end > 1) value = value.substring(1, end);
                } else {
                    int semi = value.indexOf(';');
                    if (semi >= 0) value = value.substring(0, semi).trim();
                }
                if (!value.isEmpty()) return value;
            }
        }
        String path = Uri.parse(raw).getLastPathSegment();
        return path;
    }

    private String extensionFor(String type) {
        if (type.contains("video/mp4")) return ".mp4";
        if (type.contains("video/webm")) return ".webm";
        if (type.contains("video/quicktime")) return ".mov";
        if (type.contains("audio/mpeg")) return ".mp3";
        if (type.contains("audio/mp4")) return ".m4a";
        if (type.contains("application/pdf")) return ".pdf";
        return ".bin";
    }

    private void enqueueDownload(String url, String filename, String mimeType) {
        try {
            DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager == null) throw new IllegalStateException("Service de téléchargement Android indisponible.");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setTitle(filename);
            request.setDescription("EURO DOWNLOADER Max");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            if (mimeType != null && !mimeType.isEmpty()) {
                String cleanMime = mimeType.split(";")[0].trim();
                if (cleanMime.contains("/")) request.setMimeType(cleanMime);
            }
            long id = manager.enqueue(request);
            status.setText("Téléchargement ajouté à Android.\nID : " + id + "\nFichier : Download/" + filename + "\nLe lien a été vérifié : aucune page HTML détectée.");
            Toast.makeText(this, "Téléchargement lancé", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            status.setText("Impossible de démarrer : " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        } finally {
            downloadButton.setEnabled(true);
        }
    }
}
