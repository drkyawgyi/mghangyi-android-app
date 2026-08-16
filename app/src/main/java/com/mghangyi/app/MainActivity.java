package com.mghangyi.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    // =========================================================
    // WEBSITE
    // =========================================================

    private static final String WEBSITE_URL =
            "https://mghangyi.com";

    // =========================================================
    // TELEGRAM
    // =========================================================

    private static final String TELEGRAM_URL =
            "https://t.me/aaabbbhdvip";

    // =========================================================
    // SUPABASE
    // =========================================================

    private static final String SUPABASE_URL =
            "https://vjibjebyantllmmrhuzef.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_ZkMygFtQq5bwhevExbA3Gw_B0zMaquR";

    private static final String INSTALLS_TABLE =
            "app_installs";

    // =========================================================
    // POPUNDER
    // 5 MINUTES = 300000 milliseconds
    // =========================================================

    private static final long POPUNDER_INTERVAL =
            5 * 60 * 1000L;

    private static final String PREFS_NAME =
            "mghangyi_settings";

    private static final String LAST_POPUNDER =
            "last_popunder";

    // =========================================================

    private WebView webView;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    // =========================================================
    // ACTIVITY CREATE
    // =========================================================

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        setContentView(webView);

        setupWebView();

        // Website load
        webView.loadUrl(WEBSITE_URL);

        // Register this device/install
        registerInstall();

        // Try popunder according to 5-minute rule
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                openPopUnderIfAllowed();
            }
        }, 2500);
    }

    // =========================================================
    // WEBVIEW SETUP
    // =========================================================

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(true);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(true);

        settings.setMediaPlaybackRequiresUserGesture(false);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            cookieManager.setAcceptThirdPartyCookies(
                    webView,
                    true
            );
        }

        // =====================================================
        // WEBVIEW CLIENT
        // =====================================================

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();

                return handleUrl(uri);
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url) {

                Uri uri = Uri.parse(url);

                return handleUrl(uri);
            }
        });

        // =====================================================
        // CHROME CLIENT
        // =====================================================

        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    android.os.Message resultMsg) {

                // Prevent unlimited pop-up windows.
                // Only allow one external popunder
                // according to 5-minute limit.

                if (!canOpenPopUnder()) {
                    return false;
                }

                WebView popup = new WebView(MainActivity.this);

                WebSettings popupSettings =
                        popup.getSettings();

                popupSettings.setJavaScriptEnabled(true);
                popupSettings.setDomStorageEnabled(true);

                popup.setWebViewClient(new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url) {

                        openExternalIfAllowed(url);

                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request) {

                        openExternalIfAllowed(
                                request.getUrl().toString()
                        );

                        return true;
                    }
                });

                android.webkit.WebView.WebViewTransport transport =
                        (android.webkit.WebView.WebViewTransport)
                                resultMsg.obj;

                transport.setWebView(popup);

                resultMsg.sendToTarget();

                savePopUnderTime();

                return true;
            }
        });
    }

    // =========================================================
    // URL HANDLER
    // =========================================================

    private boolean handleUrl(Uri uri) {

        if (uri == null) {
            return true;
        }

        String scheme =
                uri.getScheme() == null
                        ? ""
                        : uri.getScheme().toLowerCase();

        String host =
                uri.getHost() == null
                        ? ""
                        : uri.getHost().toLowerCase();

        // -----------------------------------------------------
        // TELEGRAM
        // -----------------------------------------------------

        if (host.equals("t.me")
                || host.equals("telegram.me")
                || host.equals("telegram.dog")) {

            openTelegram(uri);

            return true;
        }

        // -----------------------------------------------------
        // HTTP / HTTPS
        // -----------------------------------------------------

        if (scheme.equals("http")
                || scheme.equals("https")) {

            // Keep mghangyi.com inside WebView
            if (host.equals("mghangyi.com")
                    || host.endsWith(".mghangyi.com")) {

                return false;
            }

            // External websites
            openExternalIfAllowed(uri.toString());

            return true;
        }

        // -----------------------------------------------------
        // TELEGRAM APP / OTHER APP LINKS
        // -----------------------------------------------------

        if (scheme.equals("tg")) {

            openTelegram(uri);

            return true;
        }

        // -----------------------------------------------------
        // OTHER APPLICATION LINKS
        // -----------------------------------------------------

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            startActivity(intent);

        } catch (Exception ignored) {
        }

        return true;
    }

    // =========================================================
    // TELEGRAM OPEN
    // =========================================================

    private void openTelegram(Uri uri) {

        try {

            Intent telegramApp =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            telegramApp.setPackage(
                    "org.telegram.messenger"
            );

            startActivity(telegramApp);

        } catch (Exception e) {

            try {

                Intent browser =
                        new Intent(
                                Intent.ACTION_VIEW,
                                uri
                        );

                startActivity(browser);

            } catch (Exception ignored) {
            }
        }
    }

    // =========================================================
    // EXTERNAL URL
    // 5 MINUTE LIMIT
    // =========================================================

    private void openExternalIfAllowed(String url) {

        if (url == null || url.trim().isEmpty()) {
            return;
        }

        if (!canOpenPopUnder()) {
            return;
        }

        try {

            Uri uri = Uri.parse(url);

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            startActivity(intent);

            savePopUnderTime();

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // CHECK 5 MINUTE
    // =========================================================

    private boolean canOpenPopUnder() {

        long lastTime =
                getSharedPreferences(
                        PREFS_NAME,
                        MODE_PRIVATE
                ).getLong(
                        LAST_POPUNDER,
                        0
                );

        long now =
                System.currentTimeMillis();

        return (now - lastTime)
                >= POPUNDER_INTERVAL;
    }

    // =========================================================
    // SAVE POPUNDER TIME
    // =========================================================

    private void savePopUnderTime() {

        getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
        )
                .edit()
                .putLong(
                        LAST_POPUNDER,
                        System.currentTimeMillis()
                )
                .apply();
    }

    // =========================================================
    // OPEN POPUNDER
    // =========================================================

    private void openPopUnderIfAllowed() {

        if (!canOpenPopUnder()) {
            return;
        }

        /*
         * IMPORTANT:
         *
         * The actual advertising URL/script normally comes
         * from your website/ad network.
         *
         * We don't hard-code a random ad URL here.
         *
         * If your website triggers a popunder, the
         * WebViewClient / WebChromeClient above will control
         * the external opening and enforce the 5-minute limit.
         */
    }

    // =========================================================
    // INSTALL COUNT
    // =========================================================

    private void registerInstall() {

        executor.execute(new Runnable() {

            @Override
            public void run() {

                HttpURLConnection connection = null;

                try {

                    String deviceId =
                            getAppDeviceId();

                    URL url =
                            new URL(
                                    SUPABASE_URL
                                            + "/rest/v1/"
                                            + INSTALLS_TABLE
                            );

                    connection =
                            (HttpURLConnection)
                                    url.openConnection();

                    connection.setRequestMethod("POST");

                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(15000);

                    connection.setDoOutput(true);

                    connection.setRequestProperty(
                            "Content-Type",
                            "application/json"
                    );

                    connection.setRequestProperty(
                            "apikey",
                            SUPABASE_KEY
                    );

                    connection.setRequestProperty(
                            "Prefer",
                            "return=minimal"
                    );

                    JSONObject object =
                            new JSONObject();

                    object.put(
                            "device_id",
                            deviceId
                    );

                    object.put(
                            "app_version",
                            "1.0"
                    );

                    String json =
                            object.toString();

                    OutputStream output =
                            connection.getOutputStream();

                    output.write(
                            json.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

                    output.flush();
                    output.close();

                    int responseCode =
                            connection.getResponseCode();

                    if (responseCode >= 200
                            && responseCode < 300) {

                        // Successfully registered

                    } else {

                        String error =
                                readError(connection);

                        android.util.Log.e(
                                "INSTALL_COUNT",
                                "HTTP "
                                        + responseCode
                                        + " : "
                                        + error
                        );
                    }

                } catch (Exception e) {

                    android.util.Log.e(
                            "INSTALL_COUNT",
                            "Install registration error",
                            e
                    );

                } finally {

                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        });
    }

    // =========================================================
    // DEVICE ID
    // IMPORTANT:
    // Do NOT name this getDeviceId()
    // =========================================================

    @SuppressLint("HardwareIds")
    private String getAppDeviceId() {

        String androidId =
                Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ANDROID_ID
                );

        if (androidId == null
                || androidId.trim().isEmpty()) {

            return "unknown_"
                    + System.currentTimeMillis();
        }

        return androidId;
    }

    // =========================================================
    // READ ERROR
    // =========================================================

    private String readError(
            HttpURLConnection connection) {

        try {

            InputStream input =
                    connection.getErrorStream();

            if (input == null) {
                return "";
            }

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
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

            return e.getMessage() == null
                    ? ""
                    : e.getMessage();
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
    // DESTROY
    // =========================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);

            webView.destroy();

            webView = null;
        }

        executor.shutdownNow();

        mainHandler.removeCallbacksAndMessages(null);

        super.onDestroy();
    }
}
