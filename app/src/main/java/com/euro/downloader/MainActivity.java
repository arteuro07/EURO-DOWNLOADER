package com.euro.downloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
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

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private EditText urlInput;
    private TextView status;

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(30, 40, 55));
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
        root.setPadding(dp(22), dp(28), dp(22), dp(24));
        root.setBackgroundColor(Color.rgb(245, 248, 252));
        scroll.addView(root);

        TextView brand = text("EURO", 16, true);
        brand.setTextColor(Color.rgb(0, 110, 230));
        root.addView(brand);

        space(root, 8);
        root.addView(text("DOWNLOADER Max", 28, true));
        space(root, 8);
        root.addView(text(
            "Télécharge tes fichiers dans le dossier public Download de ton téléphone.",
            15, false
        ));

        space(root, 28);
        root.addView(text("Lien du fichier", 16, true));
        space(root, 8);

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(15);
        urlInput.setHint("https://exemple.com/fichier.zip");
        urlInput.setInputType( android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        urlInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        urlInput.setBackgroundColor(Color.WHITE);
        root.addView(urlInput, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        space(root, 14);
        Button download = new Button(this);
        download.setText("Télécharger");
        root.addView(download, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(54)));

        space(root, 24);
        root.addView(text("État du téléchargement", 17, true));
        space(root, 8);

        status = text(
            "Prêt. Colle un lien direct vers un fichier autorisé.",
            14, false
        );
        status.setPadding(dp(14), dp(14), dp(14), dp(14));
        status.setBackgroundColor(Color.WHITE);
        root.addView(status, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        space(root, 24);
        root.addView(text("Fonctions prévues pour Max", 17, true));
        space(root, 8);
        root.addView(text(
            "• Téléchargements en arrière-plan\n" +
            "• Notifications Android\n" +
            "• Enregistrement dans Download\n" +
            "• Interface adaptée au thème Android\n" +
            "• Extensions futures : file d'attente, historique et outils média",
            14, false
        ));

        download.setOnClickListener(v -> startDownload());
        setContentView(scroll);
    }

    private void startDownload() {
        String raw = urlInput.getText().toString().trim();
        Uri uri = Uri.parse(raw);
        String scheme = uri.getScheme();

        if (raw.isEmpty() || scheme == null ||
                !(scheme.equalsIgnoreCase("https") ||
                  scheme.equalsIgnoreCase("http")) ||
                uri.getHost() == null) {
            status.setText("Lien invalide. Utilise une URL HTTP ou HTTPS complète.");
            return;
        }

        try {
            InputMethodManager imm =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);

            String filename = uri.getLastPathSegment();
            if (filename != null) {
                filename = URLDecoder.decode(filename, StandardCharsets.UTF_8.name());
            }
            if (filename == null || filename.trim().isEmpty() ||
                    filename.equals(".") || filename.equals("..")) {
                filename = "euro_download_" + System.currentTimeMillis() + ".bin";
            }
            filename = filename.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
            if (filename.length() > 120) filename = filename.substring(0, 120);
            filename = System.currentTimeMillis() + "_" + filename;

            DownloadManager manager =
                (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (manager == null) {
                status.setText("Le service de téléchargement Android est indisponible.");
                return;
            }

            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle(filename);
            request.setDescription("EURO DOWNLOADER Max");
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, filename);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);

            long id = manager.enqueue(request);
            status.setText("Téléchargement ajouté à Android.\nID : " + id +
                    "\nFichier : Download/" + filename +
                    "\nLa notification Android indiquera le résultat.");
            Toast.makeText(this, "Téléchargement lancé", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            status.setText("Impossible de démarrer : " +
                    (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
    }
}
