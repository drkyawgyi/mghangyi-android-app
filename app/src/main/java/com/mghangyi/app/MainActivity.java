package com.mghangyi.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipe;

    private static final String HOME =
            "https://mghangyi.com/";

    // =====================================================
    // SUPABASE
    // =====================================================

    private static final String SUPABASE_URL =
            "https://vjbjebyantllmmrhuzef.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_ZkMygFtQq5bwhevExbA3Gw_B0zMaquR";

    // =====================================================
    // EXTERNAL CHROME
    // =====================================================

    private boolean openedExternal = false;

    // =====================================================
    // PREFERENCES
    // =====================================================

    private SharedPreferences prefs;

    private static final String PREF_NAME =
            "app_settings";

    private static final String RETURN_COUNT =
            "return_count";

    // =====================================================
    // POP UNDER
    // 5 MINUTES = 300,000 milliseconds
    // =====================================================

    private static final String POPUNDER_LAST_TIME =
            "popunder_last_time";

    private static final long POPUNDER_INTERVAL_MS =
            5L * 60L * 1000L;

    // =====================================================
    // INSTALL
    // =====================================================

    private static final String INSTALL_REGISTERED =
            "install_registered";

    // =====================================================
    // ON CREATE
    // =====================================================

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        swipe = findViewById(R.id.swipe);
        webView = findViewById(R.id.webview);

        prefs = getSharedPreferences(
                PREF_NAME,
                MODE_PRIVATE
        );

        // Register app installation
        registerAppInstall();

        // =================================================
        // WEBVIEW SETTINGS
        // =================================================

        WebSettings settings =
                webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);

        settings.setMediaPlaybackRequiresUserGesture(
                false
        );

        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // Popup support
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(
                true
        );

        // =================================================
        // WEBVIEW CLIENT
        // =================================================

        webView.setWebViewClient(
                new WebViewClient() {

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            WebResourceRequest request) {

                        Uri uri = request.getUrl();

                        if (isInternalUrl(uri)) {
                            return false;
                        }

                        openExternal(uri);
                        return true;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url) {

                        Uri uri = Uri.parse(url);

                        if (isInternalUrl(uri)) {
                            return false;
                        }

                        openExternal(uri);
                        return true;
                    }

                    @Override
                    public void onPageStarted(
                            WebView view,
                            String url,
                            Bitmap favicon) {

                        swipe.setRefreshing(true);
                    }

                    @Override
                    public void onPageFinished(
                            WebView view,
                            String url) {

                        swipe.setRefreshing(false);
                    }
                }
        );

        // =================================================
        // POP UNDER / NEW WINDOW
        // =================================================

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public boolean onCreateWindow(
                            WebView view,
                            boolean isDialog,
                            boolean isUserGesture,
                            Message resultMsg) {

                        // 5 minutes မပြည့်သေးရင်
                        // popup ကို မဖွင့်ပါ
                        if (!canOpenPopUnder()) {
                            return false;
                        }

                        // Popup ဖွင့်ပြီးချိန်ကို မှတ်ထား
                        markPopUnderOpened();

                        WebView popupWebView =
                                new WebView(
                                        MainActivity.this
                                );

                        WebSettings popupSettings =
                                popupWebView.getSettings();

                        popupSettings.setJavaScriptEnabled(
                                true
                        );

                        popupSettings.setDomStorageEnabled(
                                true
                        );

                        popupWebView.setWebViewClient(
                                new WebViewClient() {

                                    private boolean opened =
                                            false;

                                    @Override
                                    public void onPageStarted(
                                            WebView view,
                                            String url,
                                            Bitmap favicon) {

                                        if (opened) {
                                            return;
                                        }

                                        if (
                                                url == null ||
                                                url.equals(
                                                        "about:blank"
                                                )
                                        ) {
                                            return;
                                        }

                                        opened = true;

                                        openExternalFromPopup(
                                                Uri.parse(url),
                                                view
                                        );
                                    }

                                    @Override
                                    public boolean
                                    shouldOverrideUrlLoading(
                                            WebView view,
                                            WebResourceRequest request) {

                                        if (!opened) {

                                            opened = true;

                                            openExternalFromPopup(
                                                    request.getUrl(),
                                                    view
                                            );
                                        }

                                        return true;
                                    }

                                    @Override
                                    public boolean
                                    shouldOverrideUrlLoading(
                                            WebView view,
                                            String url) {

                                        if (!opened) {

                                            opened = true;

                                            openExternalFromPopup(
                                                    Uri.parse(url),
                                                    view
                                            );
                                        }

                                        return true;
                                    }
                                }
                        );

                        WebView.WebViewTransport transport =
                                (WebView.WebViewTransport)
                                        resultMsg.obj;

                        transport.setWebView(
                                popupWebView
                        );

                        resultMsg.sendToTarget();

                        return true;
                    }
                }
        );

        // =================================================
        // DOWNLOAD
        // =================================================

        webView.setDownloadListener(
                (
                        url,
                        userAgent,
                        contentDisposition,
                        mimeType,
                        contentLength
                ) -> {

                    try {

                        openedExternal = true;

                        Intent intent =
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                );

                        startActivity(intent);

                    } catch (Exception e) {

                        Toast.makeText(
                                MainActivity.this,
                                "Download link could not be opened",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        // =================================================
        // PULL TO REFRESH
        // =================================================

        swipe.setOnRefreshListener(
                () -> webView.reload()
        );

        // =================================================
        // LOAD WEBSITE
        // =================================================

        webView.loadUrl(HOME);

        // =================================================
        // BACK BUTTON
        // =================================================

        getOnBackPressedDispatcher().addCallback(
                this,
                new OnBackPressedCallback(true) {

                    @Override
                    public void handleOnBackPressed() {

                        if (webView.canGoBack()) {

                            webView.goBack();

                        } else {

                            finish();
                        }
                    }
                }
        );
    }

    // =====================================================
    // SUPABASE INSTALL REGISTER
    // =====================================================

    private void registerAppInstall() {

        /*
         * ဒီ device မှာ အရင် register လုပ်ပြီးသားဆို
         * ထပ်မပို့ပါ
         */

        if (
                prefs.getBoolean(
                        INSTALL_REGISTERED,
                        false
                )
        ) {
            return;
        }

        new Thread(() -> {

            HttpURLConnection connection =
                    null;

            try {

                String deviceId =
                        Settings.Secure.getString(
                                getContentResolver(),
                                Settings.Secure.ANDROID_ID
                        );

                if (
                        deviceId == null ||
                        deviceId.trim().isEmpty()
                ) {
                    return;
                }

                URL url = new URL(
                        SUPABASE_URL +
                                "/rest/v1/app_installs"
                );

                connection =
                        (HttpURLConnection)
                                url.openConnection();

                connection.setRequestMethod("POST");

                connection.setConnectTimeout(
                        10000
                );

                connection.setReadTimeout(
                        10000
                );

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
                        "Accept",
                        "application/json"
                );

                connection.setRequestProperty(
                        "Prefer",
                        "resolution=ignore-duplicates,return=minimal"
                );

                connection.setDoOutput(true);

                String json =
                        "{\"device_id\":\"" +
                        deviceId +
                        "\"}";

                OutputStream output =
                        connection.getOutputStream();

                output.write(
                        json.getBytes("UTF-8")
                );

                output.flush();
                output.close();

                int responseCode =
                        connection.getResponseCode();

                android.util.Log.d(
                        "APP_INSTALL",
                        "Supabase response = " +
                                responseCode
                );

                if (
                        responseCode >= 200 &&
                        responseCode < 300
                ) {

                    prefs.edit()
                            .putBoolean(
                                    INSTALL_REGISTERED,
                                    true
                            )
                            .apply();

                } else {

                    runOnUiThread(
                            () -> Toast.makeText(
                                    MainActivity.this,
                                    "Install count error: " +
                                            responseCode,
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }

            } catch (Exception e) {

                android.util.Log.e(
                        "APP_INSTALL",
                        "Install registration failed",
                        e
                );

                runOnUiThread(
                        () -> Toast.makeText(
                                MainActivity.this,
                                "Install count connection error",
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    // =====================================================
    // POPUP -> CHROME
    // =====================================================

    private void openExternalFromPopup(
            Uri uri,
            WebView popupView
    ) {

        try {

            openedExternal = true;

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Link could not be opened",
                    Toast.LENGTH_SHORT
            ).show();

        } finally {

            if (popupView != null) {

                popupView.stopLoading();
                popupView.destroy();
            }
        }
    }

    // =====================================================
    // CHECK 5 MINUTES
    // =====================================================

    private boolean canOpenPopUnder() {

        long lastTime =
                prefs.getLong(
                        POPUNDER_LAST_TIME,
                        0L
                );

        // ပထမဆုံးအကြိမ်
        if (lastTime == 0L) {
            return true;
        }

        long now =
                System.currentTimeMillis();

        return (
                now - lastTime
        ) >= POPUNDER_INTERVAL_MS;
    }

    // =====================================================
    // SAVE POPUP TIME
    // =====================================================

    private void markPopUnderOpened() {

        prefs.edit()
                .putLong(
                        POPUNDER_LAST_TIME,
                        System.currentTimeMillis()
                )
                .apply();
    }

    // =====================================================
    // NORMAL EXTERNAL LINK
    // =====================================================

    private void openExternal(Uri uri) {

        try {

            openedExternal = true;

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Link could not be opened",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    // =====================================================
    // INTERNAL URL CHECK
    // =====================================================

    private boolean isInternalUrl(Uri uri) {

        if (uri == null) {
            return false;
        }

        String host = uri.getHost();

        if (host == null) {
            return false;
        }

        return host.equals(
                        "mghangyi.com"
                )
                || host.equals(
                        "www.mghangyi.com"
                )
                || host.endsWith(
                        ".mghangyi.com"
                );
    }

    // =====================================================
    // RETURN FROM CHROME
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (!openedExternal) {
            return;
        }

        openedExternal = false;

        // Spinner မပိတ်ဘဲကျန်နေတာ fix
        swipe.setRefreshing(false);

        webView.stopLoading();

        // Chrome ကနေပြန်လာရင် reload
        new Handler().postDelayed(
                () -> {

                    if (!isFinishing()) {
                        webView.reload();
                    }

                },
                300
        );

        // =================================================
        // RETURN COUNT
        // =================================================

        int count =
                prefs.getInt(
                        RETURN_COUNT,
                        0
                );

        count++;

        prefs.edit()
                .putInt(
                        RETURN_COUNT,
                        count
                )
                .apply();

        // 1, 6, 11, 16...
        if (
                count == 1 ||
                (
                        count > 1 &&
                        (count - 1) % 5 == 0
                )
        ) {

            new Handler().postDelayed(
                    () -> {

                        if (!isFinishing()) {
                            showBackNotice();
                        }

                    },
                    1000
            );
        }
    }

    // =====================================================
    // BACK NOTICE
    // =====================================================

    private void showBackNotice() {

        ImageView imageView =
                new ImageView(this);

        imageView.setImageResource(
                R.drawable.back_notice
        );

        imageView.setAdjustViewBounds(true);

        imageView.setScaleType(
                ImageView.ScaleType.FIT_CENTER
        );

        int padding = 15;

        imageView.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        AlertDialog dialog =
                new AlertDialog.Builder(this)
                        .setView(imageView)
                        .setCancelable(true)
                        .create();

        dialog.show();

        if (dialog.getWindow() != null) {

            int width =
                    (int) (
                            getResources()
                                    .getDisplayMetrics()
                                    .widthPixels
                                    * 0.90
                    );

            dialog.getWindow().setLayout(
                    width,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    // =====================================================
    // DESTROY
    // =====================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.setWebViewClient(null);

            webView.setWebChromeClient(null);

            webView.destroy();
        }

        super.onDestroy();
    }
}
