package com.inase.android.gocci.ui.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.inase.android.gocci.R;
import com.inase.android.gocci.consts.Const;

import butterknife.Bind;
import butterknife.ButterKnife;

public class WebViewActivity extends AppCompatActivity {

    @Bind(R.id.tool_bar)
    Toolbar mToolBar;
    @Bind(R.id.web_view)
    WebView mWebView;

    private int category;
    // rule 0 : policy 1 : license 2

    private static MobileAnalyticsManager analytics;

    private static final String URL_RULE = "http://inase-inc.jp/rules/";
    private static final String URL_POLICY = "http://inase-inc.jp/rules/privacy/";

    public static void startWebViewActivity(int category, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, WebViewActivity.class);
        intent.putExtra("category", category);
        startingActivity.startActivity(intent);
        startingActivity.overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    this.getApplicationContext(),
                    Const.ANALYTICS_ID, //Amazon Mobile Analytics App ID
                    Const.IDENTITY_POOL_ID //Amazon Cognito Identity Pool ID
            );
        } catch (InitializationException ex) {
            Log.e(this.getClass().getName(), "Failed to initialize Amazon Mobile Analytics", ex);
        }

        setContentView(R.layout.activity_web_view);
        ButterKnife.bind(this);

        Intent intent = getIntent();
        category = intent.getIntExtra("category", 0);

        setSupportActionBar(mToolBar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //リンクをタップしたときに標準ブラウザを起動させない
        mWebView.setWebViewClient(new WebViewClient());

        switch (category) {
            case 0:
                mWebView.loadUrl(URL_RULE);
                getSupportActionBar().setTitle(getString(R.string.rule));
                break;
            case 1:
                mWebView.loadUrl(URL_POLICY);
                getSupportActionBar().setTitle(getString(R.string.policy));
                break;
            case 2:
                mWebView.loadUrl("file:///android_asset/license.html");
                getSupportActionBar().setTitle(getString(R.string.source));
                break;
        }

        //jacascriptを許可する
        mWebView.getSettings().setJavaScriptEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (analytics != null) {
            analytics.getSessionClient().pauseSession();
            analytics.getEventClient().submitEvents();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (analytics != null) {
            analytics.getSessionClient().resumeSession();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
    }
}
