package com.mghangyi.app;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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

    // Chrome / External App ဖွင့်ထားခြင်းရှိမရှိ
    private boolean externalAppOpened = false;

    // Refresh မလုပ်ခင် Scroll Position သိမ်းရန်
    private int savedScrollX = 0;
    private int savedScrollY = 0;


    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        swipe = findViewById(R.id.swipe);
        webView = findViewById(R.id.webview);


        // =========================
        // WebView Settings
        // =========================

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);

        settings.setAllowFileAccess(true);

        settings.setMediaPlaybackRequiresUserGesture(false);

        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        settings.setDisplayZoomControls(false);


        // =========================
        // WebView Client
        // =========================

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request) {

                Uri uri = request.getUrl();

                String host = uri.getHost();


                // ==================================
                // MGHANGYI Website
                // ==================================

                if (host != null &&
                        (
                                host.equals("mghangyi.com")
                                || host.endsWith(".mghangyi.com")
                        )
                ) {

                    // Website အတွင်း Link ဖြစ်ရင်
                    // WebView ထဲမှာပဲ ဖွင့်မယ်

                    return false;
                }


                // ==================================
                // External Link
                // ==================================

                showBackNotice(uri);

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


                // ==================================
                // Scroll Position Restore
                // ==================================

                if (savedScrollY > 0) {

                    webView.postDelayed(() -> {

                        webView.scrollTo(
                                savedScrollX,
                                savedScrollY
                        );

                    }, 300);

                }
            }
        });


        // =========================
        // Download Listener
        // =========================

        webView.setDownloadListener(
                (url,
                 userAgent,
                 contentDisposition,
                 mimeType,
                 contentLength) -> {

                    try {

                        externalAppOpened = true;

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


        // =========================
        // Pull To Refresh
        // =========================

        swipe.setOnRefreshListener(() -> {

            savedScrollX = webView.getScrollX();
            savedScrollY = webView.getScrollY();

            webView.reload();
        });


        // =========================
        // Load Website
        // =========================

        webView.loadUrl(HOME);


        // =========================
        // Android Back Button
        // =========================

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


    // ==================================================
    // Back Notice Popup
    // ==================================================

    private void showBackNotice(Uri uri) {

        try {

            // Scroll Position သိမ်းမယ်
            savedScrollX = webView.getScrollX();
            savedScrollY = webView.getScrollY();


            // ImageView
            ImageView imageView = new ImageView(this);

            imageView.setImageResource(
                    R.drawable.back_notice
            );

            imageView.setAdjustViewBounds(true);

            imageView.setScaleType(
                    ImageView.ScaleType.FIT_CENTER
            );


            // Popup Padding
            imageView.setPadding(
                    20,
                    10,
                    20,
                    10
            );


            // =========================
            // Dialog
            // =========================

            AlertDialog dialog =
                    new AlertDialog.Builder(this)
                            .setView(imageView)

                            .setPositiveButton(
                                    "ဆက်ကြည့်မယ်",
                                    null
                            )

                            .setNegativeButton(
                                    "မလုပ်တော့ပါ",
                                    null
                            )

                            .create();


            // =========================
            // ဆက်ကြည့်မယ်
            // =========================

            dialog.setOnShowListener(
                    d -> {

                        dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        ).setOnClickListener(v -> {

                            // External App ဖွင့်ထားကြောင်း မှတ်ထား
                            externalAppOpened = true;


                            try {

                                Intent intent =
                                        new Intent(
                                                Intent.ACTION_VIEW,
                                                uri
                                        );

                                startActivity(intent);

                                dialog.dismiss();

                            } catch (Exception e) {

                                Toast.makeText(
                                        MainActivity.this,
                                        "Link ကို ဖွင့်လို့မရပါ",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }

                        });
                    }
            );


            dialog.show();


        } catch (Exception e) {

            // Popup မပြနိုင်ရင်
            // Link ကို တိုက်ရိုက်ဖွင့်

            try {

                externalAppOpened = true;

                startActivity(
                        new Intent(
                                Intent.ACTION_VIEW,
                                uri
                        )
                );

            } catch (Exception ignored) {
            }
        }
    }


    // ==================================================
    // App ပြန်ဝင်လာတဲ့အချိန်
    // ==================================================

    @Override
    protected void onResume() {

        super.onResume();


        // Chrome / External App ကနေ
        // MGHANGYI App ပြန်ဝင်လာတာဆိုရင်

        if (externalAppOpened) {

            externalAppOpened = false;


            // နည်းနည်းစောင့်ပြီး Refresh

            webView.postDelayed(() -> {

                if (webView != null) {

                    savedScrollX = webView.getScrollX();
                    savedScrollY = webView.getScrollY();

                    webView.reload();
                }

            }, 500);
        }
    }


    // ==================================================
    // Destroy
    // ==================================================

    @Override
    protected void onDestroy() {

        if (webView != null) {

            webView.stopLoading();
            webView.destroy();
        }

        super.onDestroy();
    }
}
