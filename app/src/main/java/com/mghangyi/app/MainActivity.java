public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipe;

    private static final String HOME = "https://mghangyi.com/";

    // Chrome / external app ဖွင့်ထားလား
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

                // ကိုယ့် Website
                if (host != null &&
                        (host.equals("mghangyi.com")
                        || host.endsWith(".mghangyi.com"))) {

                    return false;
                }

                // External link -> Chrome
                try {
                    externalAppOpened = true;

                    Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            uri
                    );

                    startActivity(intent);

                } catch (Exception ignored) {}

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

        webView.setDownloadListener(
                (url, userAgent, contentDisposition,
                 mimeType, contentLength) -> {

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
        });

        swipe.setOnRefreshListener(() -> {
            webView.reload();
        });

        webView.loadUrl(HOME);

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

    // Chrome ကနေ App ထဲပြန်ဝင်လာတဲ့အချိန်
    @Override
    protected void onResume() {
        super.onResume();

        if (externalAppOpened) {

            externalAppOpened = false;

            // နည်းနည်းစောင့်ပြီး Reload
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
