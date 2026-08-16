package com.mghangyi.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    // =====================================================
    // WEBSITE
    // =====================================================

    private static final String WEBSITE_URL =
            "https://mghangyi.com/";

    // =====================================================
    // SUPABASE
    // =====================================================

    private static final String SUPABASE_URL =
            "https://vjbjebyantllmmrhuzef.supabase.co";

    private static final String SUPABASE_KEY =
            "sb_publishable_ZkMygFtQq5bwhevExbA3Gw_B0zMaquR";

    // =====================================================
    // TELEGRAM
    // =====================================================

    private static final String TELEGRAM_LINK =
            "https://t.me/aaabbbhdvip";

    // =====================================================
    // POPUNDER
    // 5 minutes = 300000 milliseconds
    // =====================================================

    private static final long POPUNDER_INTERVAL =
            5 * 60 * 1000L;

    private long lastPopunderTime = 0;


    // =====================================================
    // ON CREATE
    // =====================================================

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        setContentView(webView);

        WebSettings settings = webView.getSettings();

        // JavaScript
        settings.setJavaScriptEnabled(true);

        // Storage
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        // Zoom
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        // File access
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        // Popup
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        // Media
        settings.setMediaPlaybackRequiresUserGesture(false);

        // Cookies
        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);

        cookieManager.setAcceptThirdPartyCookies(
                webView,
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

                        return handleUrl(
                                request.getUrl().toString()
                        );
                    }


                    @Override
                    public boolean shouldOverrideUrlLoading(
                            WebView view,
                            String url) {

                        return handleUrl(url);
                    }
                }
        );


        // =================================================
        // CHROME CLIENT
        // =================================================

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public boolean onCreateWindow(
                            WebView view,
                            boolean isDialog,
                            boolean isUserGesture,
                            android.os.Message resultMsg) {

                        /*
                         * Block automatic popup windows.
                         *
                         * This prevents random popup windows
                         * from opening Chrome.
                         */

                        return false;
                    }
                }
        );


        // =================================================
        // LOAD WEBSITE
        // =================================================

        webView.loadUrl(WEBSITE_URL);


        // =================================================
        // REGISTER INSTALL
        // =================================================

        registerInstall();
    }


    // =====================================================
    // URL HANDLER
    // =====================================================

    private boolean handleUrl(String url) {

        if (url == null || url.trim().isEmpty()) {
            return true;
        }

        String lowerUrl = url.toLowerCase();


        // =================================================
        // TELEGRAM
        // =================================================

        if (lowerUrl.startsWith("https://t.me/")
                || lowerUrl.startsWith("http://t.me/")
                || lowerUrl.startsWith("https://telegram.me/")
                || lowerUrl.startsWith("http://telegram.me/")
                || lowerUrl.startsWith("tg://")) {

            openTelegram(url);

            return true;
        }


        // =================================================
        // INTENT URL
        // =================================================

        if (lowerUrl.startsWith("intent://")) {

            try {

                Intent intent =
                        Intent.parseUri(
                                url,
                                Intent.URI_INTENT_SCHEME
                        );

                if (intent != null) {

                    startActivity(intent);

                }

            } catch (Exception ignored) {
            }

            return true;
        }


        // =================================================
        // OUR WEBSITE
        // =================================================

        if (lowerUrl.startsWith("https://mghangyi.com")
                || lowerUrl.startsWith("http://mghangyi.com")) {

            webView.loadUrl(url);

            return true;
        }


        // =================================================
        // OTHER HTTP / HTTPS LINKS
        // =================================================

        if (lowerUrl.startsWith("https://")
                || lowerUrl.startsWith("http://")) {

            /*
             * Keep normal website links inside WebView.
             *
             * This prevents normal website navigation
             * from being blocked.
             */

            webView.loadUrl(url);

            return true;
        }


        return true;
    }


    // =====================================================
    // TELEGRAM
    // =====================================================

    private void openTelegram(String url) {

        try {

            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );

            startActivity(intent);

        } catch (Exception e) {

            try {

                Intent browserIntent =
                        new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(TELEGRAM_LINK)
                        );

                startActivity(browserIntent);

            } catch (Exception ignored) {

                Toast.makeText(
                        MainActivity.this,
                        "Telegram ဖွင့်၍မရပါ",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }


    // =====================================================
    // INSTALL COUNT
    // =====================================================

    private void registerInstall() {

        try {

            String deviceId =
                    Settings.Secure.getString(
                            getContentResolver(),
                            Settings.Secure.ANDROID_ID
                    );


            if (deviceId == null
                    || deviceId.trim().isEmpty()) {

                return;
            }


            // =============================================
            // LOCAL DUPLICATE PROTECTION
            // =============================================

            android.content.SharedPreferences prefs =
                    getSharedPreferences(
                            "install_data",
                            Context.MODE_PRIVATE
                    );


            boolean alreadyRegistered =
                    prefs.getBoolean(
                            "registered",
                            false
                    );


            if (alreadyRegistered) {

                return;
            }


            // =============================================
            // SEND TO SUPABASE
            // =============================================

            new Thread(
                    () -> {

                        HttpURLConnection connection =
                                null;

                        try {

                            URL url =
                                    new URL(
                                            SUPABASE_URL
                                                    + "/rest/v1/app_installs"
                                    );


                            connection =
                                    (HttpURLConnection)
                                            url.openConnection();


                            connection.setRequestMethod("POST");

                            connection.setDoOutput(true);

                            connection.setDoInput(true);


                            // =================================
                            // HEADERS
                            // =================================

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
                                    "return=minimal"
                            );


                            // =================================
                            // JSON
                            // =================================

                            JSONObject json =
                                    new JSONObject();


                            json.put(
                                    "device_id",
                                    deviceId
                            );


                            String data =
                                    json.toString();


                            // =================================
                            // SEND
                            // =================================

                            OutputStream output =
                                    connection.getOutputStream();


                            output.write(
                                    data.getBytes("UTF-8")
                            );


                            output.flush();

                            output.close();


                            // =================================
                            // RESPONSE
                            // =================================

                            int responseCode =
                                    connection.getResponseCode();


                            if (responseCode >= 200
                                    && responseCode < 300) {


                                // =============================
                                // SUCCESS
                                // =============================

                                prefs.edit()
                                        .putBoolean(
                                                "registered",
                                                true
                                        )
                                        .apply();


                            } else {


                                // =============================
                                // ERROR BODY
                                // =============================

                                String errorText =
                                        readErrorResponse(
                                                connection
                                        );


                                runOnUiThread(
                                        () -> {

                                            Toast.makeText(
                                                    MainActivity.this,
                                                    "Install count error: "
                                                            + responseCode
                                                            + "\n"
                                                            + errorText,
                                                    Toast.LENGTH_LONG
                                            ).show();

                                        }
                                );
                            }


                        } catch (Exception e) {


                            runOnUiThread(
                                    () -> {

                                        Toast.makeText(
                                                MainActivity.this,
                                                "Install count connection error",
                                                Toast.LENGTH_LONG
                                        ).show();

                                    }
                            );


                        } finally {

                            if (connection != null) {

                                connection.disconnect();
                            }
                        }

                    }
            ).start();


        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =====================================================
    // READ SUPABASE ERROR
    // =====================================================

    private String readErrorResponse(
            HttpURLConnection connection) {

        try {

            InputStream inputStream;

            if (connection.getErrorStream() != null) {

                inputStream =
                        connection.getErrorStream();

            } else {

                inputStream =
                        connection.getInputStream();
            }


            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    inputStream
                            )
                    );


            StringBuilder result =
                    new StringBuilder();


            String line;


            while (
                    (line = reader.readLine()) != null
            ) {

                result.append(line);
            }


            reader.close();


            return result.toString();


        } catch (Exception e) {

            return "Unknown Supabase error";
        }
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    @Override
    public void onBackPressed() {

        if (webView != null
                && webView.canGoBack()) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }


    // =====================================================
    // DESTROY
    // =====================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.destroy();
        }


        super.onDestroy();
    }
}
