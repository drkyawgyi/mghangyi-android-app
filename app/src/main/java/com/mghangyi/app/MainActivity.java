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

    // Website
    private static final String WEBSITE_URL =
            "https://mghangyi.com/";

    // Supabase Project URL
    private static final String SUPABASE_URL =
            "https://vjbjebyantllmmrhuzef.supabase.co";

    // Supabase Publishable Key
    private static final String SUPABASE_KEY =
            "sb_publishable_ZkMygFtQq5bwhevExbA3Gw_B0zMaquR";

    // Telegram VIP
    private static final String TELEGRAM_LINK =
            "https://t.me/aaabbbhdvip";


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);

        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        // Block popup / popunder new windows
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);

        CookieManager cookieManager =
                CookieManager.getInstance();

        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(
                webView,
                true
        );


        // WebView
        webView.setWebViewClient(new WebViewClient() {

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
        });


        // Block new popup windows
        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public boolean onCreateWindow(
                            WebView view,
                            boolean isDialog,
                            boolean isUserGesture,
                            android.os.Message resultMsg) {

                        return false;
                    }
                }
        );


        // Open website
        webView.loadUrl(WEBSITE_URL);


        // Register install
        registerInstall();
    }


    // =====================================================
    // URL HANDLER
    // =====================================================

    private boolean handleUrl(String url) {

        if (url == null) {
            return true;
        }


        // Telegram
        if (url.startsWith("https://t.me/")
                || url.startsWith("http://t.me/")
                || url.startsWith("https://telegram.me/")
                || url.startsWith("http://telegram.me/")
                || url.startsWith("tg://")) {

            openTelegram(url);

            return true;
        }


        // Block intent popup
        if (url.startsWith("intent://")) {
            return true;
        }


        // Website links
        if (url.startsWith("https://mghangyi.com")
                || url.startsWith("http://mghangyi.com")) {

            webView.loadUrl(url);

            return true;
        }


        // Block unknown external popups
        return true;
    }


    // =====================================================
    // TELEGRAM
    // =====================================================

    private void openTelegram(String url) {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Telegram ဖွင့်၍မရပါ",
                    Toast.LENGTH_SHORT
            ).show();
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

            if (deviceId == null ||
                    deviceId.trim().isEmpty()) {
                return;
            }


            // Prevent duplicate registration
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


            new Thread(() -> {

                HttpURLConnection connection = null;

                try {

                    URL url = new URL(
                            SUPABASE_URL
                                    + "/rest/v1/app_installs"
                    );

                    connection =
                            (HttpURLConnection)
                                    url.openConnection();

                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setDoInput(true);

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


                    // JSON
                    JSONObject json =
                            new JSONObject();

                    json.put(
                            "device_id",
                            deviceId
                    );

                    String data =
                            json.toString();


                    OutputStream output =
                            connection.getOutputStream();

                    output.write(
                            data.getBytes("UTF-8")
                    );

                    output.flush();
                    output.close();


                    int responseCode =
                            connection.getResponseCode();


                    // SUCCESS
                    if (responseCode >= 200 &&
                            responseCode < 300) {

                        prefs.edit()
                                .putBoolean(
                                        "registered",
                                        true
                                )
                                .apply();

                    }


                    // ERROR
                    else {

                        runOnUiThread(() -> {

                            Toast.makeText(
                                    MainActivity.this,
                                    "Install count error: "
                                            + responseCode,
                                    Toast.LENGTH_LONG
                            ).show();

                        });
                    }


                } catch (Exception e) {

                    runOnUiThread(() -> {

                        Toast.makeText(
                                MainActivity.this,
                                "Install count connection error",
                                Toast.LENGTH_LONG
                        ).show();

                    });

                } finally {

                    if (connection != null) {
                        connection.disconnect();
                    }
                }

            }).start();


        } catch (Exception e) {

            e.printStackTrace();
        }
    }


    // =====================================================
    // BACK BUTTON
    // =====================================================

    @Override
    public void onBackPressed() {

        if (webView != null &&
                webView.canGoBack()) {

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
