package com.mghangyi.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipe;

    private static final String HOME = "https://mghangyi.com/";

    // External link / Chrome ဖွင့်ထားခြင်း
    private boolean openedExternal = false;

    // Notice count သိမ်းရန်
    private SharedPreferences prefs;

    private static final String PREF_NAME = "app_settings";
    private static final String RETURN_COUNT = "return_count";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        swipe = findViewById(R.id.swipe);
        webView = findViewById(R.id.webview);

        prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);

        s.setMediaPlaybackRequiresUserGesture(false);

        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {

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
        });


        // Download link
        webView.setDownloadListener(
                (url, userAgent, contentDisposition,
                 mimeType, contentLength) -> {

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


        // User ကိုယ်တိုင် pull down လုပ်ရင်သာ refresh
        swipe.setOnRefreshListener(() -> {

            webView.reload();

        });


        // Website စတင်ဖွင့်
        webView.loadUrl(HOME);


        // Android Back Button
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


    // =========================================================
    // External URL ဖွင့်ခြင်း
    // =========================================================

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


    // =========================================================
    // Website အတွင်း Link ဟုတ်/မဟုတ်
    // =========================================================

    private boolean isInternalUrl(Uri uri) {

        if (uri == null) {

            return false;
        }

        String host = uri.getHost();

        if (host == null) {

            return false;
        }

        return host.equals("mghangyi.com")
                || host.equals("www.mghangyi.com")
                || host.endsWith(".mghangyi.com");
    }


    // =========================================================
    // Chrome / Ad ကနေ Back ပြန်ဝင်လာတဲ့အချိန်
    // =========================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (!openedExternal) {

            return;
        }

        // External app ပြန်လာပြီ
        openedExternal = false;


        // Loading spinner ကို အရင်ဆုံးပိတ်
        swipe.setRefreshing(false);

        // WebView ရဲ့ stuck loading ကို ရပ်
        webView.stopLoading();


        /*
         * Chrome ကနေ Back ပြန်လာတဲ့အခါ
         * WebView ကို clean reload တစ်ကြိမ်လုပ်ပေးမယ်။
         *
         * ဒီလိုလုပ်မှ screenshot ထဲက
         * အဝိုင်းကြီးလည်ပြီး မပြီးတဲ့ပြဿနာ
         * ပျောက်သွားမယ်။
         */

        new Handler().postDelayed(() -> {

            if (!isFinishing()) {

                webView.reload();

            }

        }, 300);


        // =====================================================
        // Notice Counter
        // =====================================================

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


        /*
         * Notice ပြမယ့် အစီအစဉ်
         *
         * 1
         * 6
         * 11
         * 16
         * 21
         * ...
         */

        if (count == 1 || (count > 1 && (count - 1) % 5 == 0)) {

            // Page reload ပြီးမှ Notice ပြ
            new Handler().postDelayed(() -> {

                if (!isFinishing()) {

                    showBackNotice();
                }

            }, 1000);
        }
    }


    // =========================================================
    // Back Notice
    // =========================================================

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

            dialog.getWindow()
                    .setLayout(
                            width,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    );
        }
    }


    // =========================================================
    // Destroy
    // =========================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();

            webView.setWebViewClient(null);

            webView.destroy();
        }

        super.onDestroy();
    }
}
