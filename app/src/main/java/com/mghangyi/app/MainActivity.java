package com.mghangyi.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.*;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipe;

    private static final String HOME = "https://mghangyi.com/";

    // External app / Chrome ပွင့်သွားခဲ့လား စစ်ရန်
    private boolean externalAppOpened = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swipe = findViewById(R.id.swipe);
        webView = findViewById(R.id.webview);

        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();
                String host = uri.getHost();

                // ကိုယ့် website ထဲက link ဆို WebView ထဲမှာပဲ ဖွင့်
                if (host != null &&
                        (host.equals("mghangyi.com")
                                || host.endsWith(".mghangyi.com"))) {

                    return false;
                }

                // External link / Chrome / Ads
                try {
                    externalAppOpened = true;

                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

                    startActivity(intent);

                } catch (Exception ignored) {
                }

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
                (url, userAgent, contentDisposition, mimeType, contentLength) -> {

                    try {

                        externalAppOpened = true;

                        startActivity(
                                new Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(url)
                                )
                        );

                    } catch (Exception e) {

                        Toast.makeText(
                                this,
                                "Download link could not be opened",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        // Pull to refresh
        swipe.setOnRefreshListener(
                () -> webView.reload()
        );

        // Website load
        webView.loadUrl(HOME);

        // Android Back button
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

    // Chrome / External App ကနေ App ထဲပြန်ဝင်လာတဲ့အချိန်
    @Override
    protected void onResume() {

        super.onResume();

        if (externalAppOpened) {

            externalAppOpened = false;

            // Chrome ပိတ်ပြီး App ပြန်ဝင်တဲ့အချိန်
            // 500ms နောက်မှာ တစ်ကြိမ်ပဲ refresh
            webView.postDelayed(() -> {

                if (webView != null) {
                    webView.reload();
                }

            }, 500);
        }
    }

    @Override
    protected void onDestroy() {

        if (webView != null) {
            webView.destroy();
        }

        super.onDestroy();
    }
}
