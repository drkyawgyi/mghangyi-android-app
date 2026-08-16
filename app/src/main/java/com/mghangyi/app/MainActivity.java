package com.mghangyi.app;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    private static final String WEBSITE_URL =
            "https://mghangyi.com";

    private static final String TELEGRAM_URL =
            "https://t.me/aaabbbhdvip";

    /*
     * Supabase Project ID
     */
    private static final String SUPABASE_PROJECT_ID =
            "vjbjebyantllmmrhuzef";

    /*
     * IMPORTANT:
     * Publishable key only.
     * Never put Supabase secret/service_role key in Android app.
     */
    private static final String SUPABASE_ANON_KEY =
            "sb_publishable_ZkMygFtQq5bwhevExbA3Gw_B0zMaquR";

    private final Handler handler = new Handler();

    private boolean pageLoaded = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        setupWebView();

        /*
         * Don't let the app stay on loading forever.
         */
        handler.postDelayed(() -> {

            if (!pageLoaded) {
                Toast.makeText(
                        MainActivity.this,
                        "Internet connection is slow. Please try again.",
                        Toast.LENGTH_SHORT
                ).show();
            }

        }, 15000);

        /*
         * Install count runs separately.
         * If it fails, the app still works.
         */
        sendInstallCount();

        loadWebsite();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setMediaPlaybackRequiresUserGesture(false);

        /*
         * IMPORTANT
         * Keep navigation inside WebView except
         * Telegram / external app links.
         */
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

            @Override
            public void onPageFinished(
                    WebView view,
                    String url) {

                super.onPageFinished(view, url);

                pageLoaded = true;
            }

            @Override
            public void onReceivedError(
                    WebView view,
                    int errorCode,
                    String description,
                    String failingUrl) {

                super.onReceivedError(
                        view,
                        errorCode,
                        description,
                        failingUrl
                );

                /*
                 * Don't keep showing infinite loading.
                 */
                Toast.makeText(
                        MainActivity.this,
                        "Page loading failed. Please check your internet.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
    }

    private boolean handleUrl(String url) {

        if (url == null) {
            return false;
        }

        /*
         * Telegram links
         */
        if (url.startsWith("https://t.me/")
                || url.startsWith("http://t.me/")
                || url.startsWith("tg://")) {

            openTelegram(url);
            return true;
        }

        /*
         * Telegram username link specifically
         */
        if (url.contains("aaabbbhdvip")) {

            openTelegram(TELEGRAM_URL);
            return true;
        }

        /*
         * Normal website links stay inside WebView.
         */
        if (url.startsWith("http://")
                || url.startsWith("https://")) {

            return false;
        }

        /*
         * Other external schemes.
         */
        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Cannot open this link",
                    Toast.LENGTH_SHORT
            ).show();
        }

        return true;
    }

    private void openTelegram(String url) {

        /*
         * First try Telegram application.
         */
        try {

            Intent telegramIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            telegramIntent.setPackage("org.telegram.messenger");

            startActivity(telegramIntent);

            return;

        } catch (ActivityNotFoundException ignored) {
            // Telegram app not installed
        }

        /*
         * If Telegram isn't installed,
         * open browser.
         */
        try {

            Intent browserIntent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(browserIntent);

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Telegram link cannot be opened",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void loadWebsite() {

        pageLoaded = false;

        webView.loadUrl(WEBSITE_URL);
    }

    /*
     * ==============================
     * INSTALL COUNT
     * ==============================
     *
     * This runs in background.
     *
     * IMPORTANT:
     * We do NOT block the WebView while
     * install count is being sent.
     */
    private void sendInstallCount() {

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                String deviceId = getDeviceId(this);

                String projectUrl =
                        "https://" +
                        SUPABASE_PROJECT_ID +
                        ".supabase.co";

                /*
                 * IMPORTANT:
                 * This endpoint is only a placeholder until
                 * your actual Supabase table/RPC name is known.
                 *
                 * It intentionally does NOT block the app.
                 */

                URL url = new URL(
                        projectUrl +
                        "/rest/v1/install_counts"
                );

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");

                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                connection.setRequestProperty(
                        "apikey",
                        SUPABASE_ANON_KEY
                );

                connection.setRequestProperty(
                        "Authorization",
                        "Bearer " + SUPABASE_ANON_KEY
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
                        + escapeJson(deviceId)
                        + "\""
                        + "}";

                connection
                        .getOutputStream()
                        .write(json.getBytes("UTF-8"));

                int responseCode =
                        connection.getResponseCode();

                /*
                 * 401 / 404 / 400 should NOT affect app.
                 */
                if (responseCode >= 200
                        && responseCode < 300) {

                    // Count successfully sent.

                } else {

                    // Ignore count error.
                    // App continues normally.
                }

            } catch (Exception ignored) {

                /*
                 * Never stop the app because
                 * install counter failed.
                 */

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    /*
     * Android Device ID
     *
     * This replaces the old getDeviceId()
     * which caused your Gradle compile error.
     */
    private String getDeviceId(Context context) {

        try {

            String id = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );

            if (id != null && !id.isEmpty()) {
                return id;
            }

        } catch (Exception ignored) {
        }

        return "unknown_device";
    }

    private String escapeJson(String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {

        handler.removeCallbacksAndMessages(null);

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
    }
}
