package com.riyalo.quotation.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.webkit.WebSettings;

public class MainActivity extends Activity {

    private static final String PREFS = "quotation_app";
    private static final String KEY_SERVER = "server_url";
    private static final String DEFAULT_SERVER = "http://192.168.0.158:1120";

    private WebView webView;
    private ProgressBar progressBar;
    private SharedPreferences prefs;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        FrameLayout main = findViewById(R.id.main);

        // Loading bar shown on top while pages load
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, dp(3));
        progressBar.setLayoutParams(barParams);
        main.addView(progressBar);

        webView = new WebView(this);
        main.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient());

        loadServer();
    }

    private void loadServer() {
        String server = prefs.getString(KEY_SERVER, DEFAULT_SERVER);
        webView.loadUrl(server);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Reload");
        menu.add(0, 2, 0, "Change server address");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            loadServer();
            return true;
        }
        if (item.getItemId() == 2) {
            promptServerAddress();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void promptServerAddress() {
        String current = prefs.getString(KEY_SERVER, DEFAULT_SERVER);
        final EditText input = new EditText(this);
        input.setText(current);
        input.setSelection(current.length());

        new AlertDialog.Builder(this)
                .setTitle("Server address")
                .setMessage("Address of the computer running Quotation Generator:")
                .setView(input)
                .setPositiveButton("Connect", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String value = input.getText().toString().trim();
                        if (!value.startsWith("http://") && !value.startsWith("https://")) {
                            value = "http://" + value;
                        }
                        prefs.edit().putString(KEY_SERVER, value).apply();
                        loadServer();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private int dp(int value) {
        return Math.round(getResources().getDisplayMetrics().density * value);
    }
}