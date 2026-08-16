package com.mghangyi.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MainActivity extends Activity {

    private WebView webView;

    // ==============================
    // SUPABASE
    // ==============================

    private static final String SUPABASE_URL =
            "https://vjbjebyantllmmrhuzef.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_ZkMygFtQq5bwhevExbA3Gw_B0zMaquR";

    // ==============================
    // WEBSITE
    // ==============================

    private static final String WEBSITE_URL =
            "https://mghangyi.com";

    // ==============================
    // TELEGRAM
    // ==============================

    private static final String TELEGRAM_URL =
            "https://t.me/aaabbbhdvip";

    // ==============================
    // POPUNDER
    // ==============================

    // 5 minutes
    private static final long POPUNDER_INTERVAL =
            5 * 60 * 1000L;

    private static final String PREFS_NAME =
            "mghangyi_settings";

    private static final String LAST_POPUNDER =
            "last_popunder";

    private android.content.SharedPreferences prefs;

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        );

        setupWebView();

        // Register install
        registerInstall();
    }

    // =========================================================
    // WEBVIEW
    // =========================================================

    private void setupWebView() {

        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                String url = request.getUrl().toString();

                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                return handleUrl(url);
            }
        });

        webView.loadUrl(WEBSITE_URL);
    }

    // =========================================================
    // URL HANDLER
    // =========================================================

    private boolean handleUrl(String url) {

        if (url == null) {
            return false;
        }

        // Telegram
        if (url.startsWith("https://t.me/")
                || url.startsWith("http://t.me/")
                || url.startsWith("tg://")) {

            openTelegram(url);

            return true;
        }

        // Telegram invite links
        if (url.startsWith("https://telegram.me/")
                || url.startsWith("http://telegram.me/")) {

            openTelegram(url);

            return true;
        }

        // Normal website
        if (url.startsWith("https://mghangyi.com")
                || url.startsWith("http://mghangyi.com")) {

            webView.loadUrl(url);

            return true;
        }

        // Other external links
        try {

            Intent intent =
                    new Intent(Intent.ACTION_VIEW);

            intent.setData(Uri.parse(url));

            startActivity(intent);

            return true;

        } catch (Exception e) {

            return false;
        }
    }

    // =========================================================
    // TELEGRAM
    // =========================================================

    private void openTelegram(String url) {

        try {

            Intent intent =
                    new Intent(Intent.ACTION_VIEW);

            intent.setData(Uri.parse(url));

            startActivity(intent);

        } catch (Exception e) {

            try {

                Intent browser =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(url)
                        );

                startActivity(browser);

            } catch (Exception ignored) {
            }
        }
    }

    // =========================================================
    // INSTALL COUNT
    // =========================================================

    private void registerInstall() {

        new Thread(() -> {

            try {

                String deviceId =
                        getDeviceId();

                URL url =
                        new URL(
                                SUPABASE_URL
                                        + "/rest/v1/app_installs"
                        );

                HttpURLConnection connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setRequestProperty(
                        "apikey",
                        SUPABASE_KEY
                );

                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + SUPABASE_KEY
                );

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Prefer",
                        "return=minimal"
                );

                connection.setDoOutput(true);

                String json =
                        "{"
                                + "\"device_id\":\""
                                + deviceId
                                + "\""
                                + "}";

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        json.getBytes("UTF-8")
                );

                output.flush();
                output.close();

                int responseCode =
                        connection.getResponseCode();

                if (responseCode >= 200
                        && responseCode < 300) {

                    runOnUiThread(() -> {

                        Toast.makeText(
                                MainActivity.this,
                                "Install registered",
                                Toast.LENGTH_SHORT
                        ).show();

                    });

                } else {

                    String error =
                            readError(connection);

                    runOnUiThread(() -> {

                        Toast.makeText(
                                MainActivity.this,
                                "Install error: "
                                        + responseCode,
                                Toast.LENGTH_SHORT
                        ).show();

                    });

                }

                connection.disconnect();

            } catch (Exception e) {

                runOnUiThread(() -> {

                    Toast.makeText(
                            MainActivity.this,
                            "Install connection error",
                            Toast.LENGTH_SHORT
                    ).show();

                });
            }

        }).start();
    }

    // =========================================================
    // DEVICE ID
    // =========================================================

    private String getDeviceId() {

        android.content.SharedPreferences devicePrefs =
                getSharedPreferences(
                        "device_info",
                        MODE_PRIVATE
                );

        String id =
                devicePrefs.getString(
                        "device_id",
                        null
                );

        if (id == null) {

            id = UUID.randomUUID().toString();

            devicePrefs.edit()
                    .putString(
                            "device_id",
                            id
                    )
                    .apply();
        }

        return id;
    }

    // =========================================================
    // ERROR READER
    // =========================================================

    private String readError(
            HttpURLConnection connection) {

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    connection.getErrorStream()
                            )
                    );

            StringBuilder result =
                    new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {

                result.append(line);
            }

            reader.close();

            return result.toString();

        } catch (Exception e) {

            return "";
        }
    }

    // =========================================================
    // POPUNDER - 5 MINUTES
    // =========================================================

    public void openPopunder(String adUrl) {

        long now =
                System.currentTimeMillis();

        long last =
                prefs.getLong(
                        LAST_POPUNDER,
                        0
                );

        // Already opened within 5 minutes
        if (now - last < POPUNDER_INTERVAL) {
            return;
        }

        prefs.edit()
                .putLong(
                        LAST_POPUNDER,
                        now
                )
                .apply();

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(adUrl)
                    );

            startActivity(intent);

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // BACK BUTTON
    // =========================================================

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    // =========================================================
    // CLEANUP
    // =========================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
    }
}
