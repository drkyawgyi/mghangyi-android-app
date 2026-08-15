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

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipe;

    private static final String HOME = "https://mghangyi.com/";

    // External link ကနေ Chrome ပြန်ဝင်လာတာကို မှတ်ရန်
    private boolean openedExternal = false;

    // နောက်တစ်ကြိမ် Notice ပြမယ့် click အရေအတွက်
    private int clickCount = 0;

    // 4 ~ 6 ကြိမ်ကြား random
    private int nextNoticeAt;

    private final Random random = new Random();

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        swipe = findViewById(R.id.swipe);
        webView = findViewById(R.id.webview);

        // ပထမဆုံး Notice ပြမယ့်အကြိမ်ကို random သတ်မှတ်
        nextNoticeAt = randomNoticeNumber();

        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);

        // Video တွေကို user interaction မလိုဘဲ play ခွင့်
        s.setMediaPlaybackRequiresUserGesture(false);

        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    WebResourceRequest request
            ) {

                Uri uri = request.getUrl();

                if (isInternalUrl(uri)) {
                    // mghangyi.com ထဲက link
                    return false;
                }

                // External link / Ad / Chrome
                openExternalLink(uri);

                return true;
            }

            // Android version အဟောင်းတွေအတွက်
            @Override
            public boolean shouldOverrideUrlLoading(
                    WebView view,
                    String url
            ) {

                Uri uri = Uri.parse(url);

                if (isInternalUrl(uri)) {
                    return false;
                }

                openExternalLink(uri);

                return true;
            }

            @Override
            public void onPageStarted(
                    WebView view,
                    String url,
                    Bitmap favicon
            ) {
                // Loading indicator ပဲပြ
                swipe.setRefreshing(true);
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                swipe.setRefreshing(false);
            }
        });

        // Download link တွေ
        webView.setDownloadListener(
                (url, userAgent, contentDisposition, mimeType, contentLength) -> {

                    try {

                        openedExternal = true;

                        Intent intent =
                                new Intent(Intent.ACTION_VIEW, Uri.parse(url));

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

        /*
         * Swipe down လုပ်ရင် ကိုယ်တိုင် Refresh လုပ်နိုင်တယ်။
         *
         * App ပြန်ဝင်လာတိုင်း Auto Refresh မလုပ်တော့ပါ။
         */
        swipe.setOnRefreshListener(() -> webView.reload());

        // Website ကို ပထမဆုံးတစ်ကြိမ် Load
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


    /*
     * Internal website ဟုတ်/မဟုတ် စစ်ခြင်း
     */
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


    /*
     * External link ကို Chrome / သက်ဆိုင်ရာ App ထဲဖွင့်
     */
    private void openExternalLink(Uri uri) {

        try {

            openedExternal = true;

            Intent intent =
                    new Intent(Intent.ACTION_VIEW, uri);

            startActivity(intent);

        } catch (Exception e) {

            Toast.makeText(
                    MainActivity.this,
                    "Link could not be opened",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }


    /*
     * Chrome / External App ကနေ
     * Main App ထဲ Back ပြန်ဝင်လာတဲ့အချိန်
     */
    @Override
    protected void onResume() {

        super.onResume();

        if (openedExternal) {

            openedExternal = false;

            // External link တစ်ကြိမ်ပြန်ဝင်လာတိုင်း count +1
            clickCount++;

            // သတ်မှတ်ထားတဲ့အကြိမ်ရောက်ပြီဆို Notice ပြ
            if (clickCount >= nextNoticeAt) {

                clickCount = 0;

                // နောက်တစ်ကြိမ်ကို 4 ~ 6 ကြိမ်ကြား random
                nextNoticeAt = randomNoticeNumber();

                showBackNotice();
            }
        }

        /*
         * IMPORTANT:
         * ဒီနေရာမှာ webView.reload() မရှိပါ။
         *
         * ဒါကြောင့် Chrome ကနေ Back ပြန်လာတဲ့အခါ
         * Website Auto Refresh မဖြစ်တော့ပါ။
         */
    }


    /*
     * 4 ~ 6 ကြိမ်ကြား Random
     */
    private int randomNoticeNumber() {

        return 4 + random.nextInt(3);
        // 4, 5, 6 ထဲက တစ်ခု random
    }


    /*
     * Back Notice ပြခြင်း
     */
    private void showBackNotice() {

        ImageView imageView = new ImageView(this);

        imageView.setImageResource(R.drawable.back_notice);

        imageView.setAdjustViewBounds(true);

        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

        int padding = 20;

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

        /*
         * Dialog ကို screen အကျယ်နဲ့ သင့်တော်အောင်
         */
        if (dialog.getWindow() != null) {

            int width =
                    (int) (getResources()
                            .getDisplayMetrics()
                            .widthPixels * 0.90);

            dialog.getWindow()
                    .setLayout(
                            width,
                            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    );
        }
    }


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
