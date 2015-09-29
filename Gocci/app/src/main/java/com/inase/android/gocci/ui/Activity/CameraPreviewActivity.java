package com.inase.android.gocci.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.andexert.library.RippleView;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;
import com.inase.android.gocci.application.Application_Gocci;
import com.inase.android.gocci.Base.SquareVideoView;
import com.inase.android.gocci.R;
import com.inase.android.gocci.common.Const;
import com.inase.android.gocci.common.SavedData;
import com.inase.android.gocci.common.Util;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import cz.msebera.android.httpclient.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.fabric.sdk.android.Fabric;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class CameraPreviewActivity extends AppCompatActivity {

    @Bind(R.id.preview_video)
    SquareVideoView mPreviewVideo;
    @Bind(R.id.tool_bar)
    Toolbar mToolBar;
    @Bind(R.id.restname_spinner)
    MaterialBetterSpinner mRestnameSpinner;
    @Bind(R.id.add_rest_view)
    RelativeLayout mAddRestView;
    @Bind(R.id.category_spinner)
    MaterialBetterSpinner mCategorySpinner;
    @Bind(R.id.mood_spinner)
    MaterialBetterSpinner mMoodSpinner;
    @Bind(R.id.edit_value)
    MaterialEditText mEditValue;
    @Bind(R.id.edit_comment)
    MaterialEditText mEditComment;
    @Bind(R.id.check_cheer)
    CheckBox mCheckCheer;
    @Bind(R.id.toukou_button_ripple)
    RippleView mToukouButtonRipple;
    @Bind(R.id.progress_wheel)
    ProgressWheel mProgressWheel;

    @OnClick(R.id.rest_add_button)
    public void restAdd() {
        createTenpo();
    }

    @OnClick(R.id.button_twitter)
    public void twitter() {
        Uri bmpUri = Util.getUri(mVideoUrl);
        if (bmpUri != null) {
            if (mRestname.equals("")) {
                Toast.makeText(CameraPreviewActivity.this, getString(R.string.please_input_restname), Toast.LENGTH_SHORT).show();
            } else {
                TweetComposer.Builder builder = new TweetComposer.Builder(CameraPreviewActivity.this)
                        .text("#" + mRestname.replaceAll("\\s+", "") + " #Gocci")
                        .image(bmpUri);

                builder.show();
            }
        } else {
            // ...sharing failed, handle error
            Toast.makeText(CameraPreviewActivity.this, getString(R.string.error_share), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.button_facebook)
    public void facebook() {
        Uri uri = Uri.fromFile(mVideoFile);
        if (ShareDialog.canShow(ShareVideoContent.class)) {
            ShareVideo video = new ShareVideo.Builder()
                    .setLocalUrl(uri)
                    .build();
            ShareVideoContent content = new ShareVideoContent.Builder()
                    .setVideo(video)
                    .build();
            shareDialog.show(content);
        } else {
            // ...sharing failed, handle error
            Toast.makeText(CameraPreviewActivity.this, getString(R.string.error_share), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.button_instagram)
    public void instagram() {
        if (!mRestname.equals("")) {
            Uri uri = Uri.fromFile(mVideoFile);
            Intent share = new Intent(Intent.ACTION_SEND);
            // Set the MIME type
            share.setType("video/*");
            // Add the URI and the caption to the Intent.
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setPackage("com.instagram.android");
            share.putExtra(Intent.EXTRA_TEXT, "#" + mRestname.replaceAll("\\s+", "") + " #Gocci");
            // Broadcast the Intent.
            startActivity(Intent.createChooser(share, "Share to"));
        } else {
            Toast.makeText(CameraPreviewActivity.this, getString(R.string.please_input_restname), Toast.LENGTH_SHORT).show();
        }
    }

    private int mRest_id;
    private int mCategory_id;
    private int mTag_id;
    private int mCheer_flag = 0;
    private String mRestname;
    private String mVideoUrl;
    private String mAwsPostName;
    private String mValue;
    private String mMemo;
    private boolean mIsnewRestname;
    private double mLatitude;
    private double mLongitude;

    private File mVideoFile;

    private CallbackManager callbackManager;
    private ShareDialog shareDialog;

    private boolean isError;

    private static MobileAnalyticsManager analytics;

    private ArrayAdapter<String> restAdapter;

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

        setContentView(R.layout.activity_camera_preview);
        ButterKnife.bind(this);

        isError = false;

        Intent intent = getIntent();
        mRest_id = intent.getIntExtra("rest_id", 1);
        mVideoUrl = intent.getStringExtra("video_url");
        mAwsPostName = intent.getStringExtra("aws") + "_" + SavedData.getServerUserId(this);
        mCategory_id = intent.getIntExtra("category_id", 1);
        mTag_id = intent.getIntExtra("tag_id", 1);
        mMemo = intent.getStringExtra("memo");
        mValue = intent.getStringExtra("value");
        mIsnewRestname = intent.getBooleanExtra("isNewRestname", false);
        mLatitude = intent.getDoubleExtra("lat", 0.0);
        mLongitude = intent.getDoubleExtra("lon", 0.0);

        SavedData.setPostVideoPreview(this, mRest_id == 1 ? "" : GocciCameraActivity.restname.get(GocciCameraActivity.rest_id.indexOf(mRest_id)),
                mRest_id, mVideoUrl, mAwsPostName, mCategory_id, mTag_id, mMemo, mValue, mIsnewRestname, mLongitude, mLatitude);

        if (mLatitude == 0.0 && mLongitude == 0.0) {
            //もう一度位置取る？
            SmartLocation.with(this).location().oneFix().start(new OnLocationUpdatedListener() {
                @Override
                public void onLocationUpdated(Location location) {
                    mLatitude = location.getLatitude();
                    mLongitude = location.getLongitude();
                    getTenpoJson(CameraPreviewActivity.this);
                    SavedData.setLat(CameraPreviewActivity.this, mLatitude);
                    SavedData.setLon(CameraPreviewActivity.this, mLongitude);
                }
            });
        }

        mVideoFile = new File(mVideoUrl);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(CameraPreviewActivity.this, getString(R.string.complete_share), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(CameraPreviewActivity.this, getString(R.string.cancel_share), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(CameraPreviewActivity.this, getString(R.string.error_share), Toast.LENGTH_SHORT).show();
            }
        });

        Fabric.with(this, new TweetComposer());

        //toolbar.inflateMenu(R.menu.toolbar_menu);
        //toolbar.setLogo(R.drawable.ic_gocci_moji_white45);
        mToolBar.setTitle("");
        setSupportActionBar(mToolBar);

        if (mIsnewRestname) {
            mAddRestView.setVisibility(View.GONE);
        }

        String[] CATEGORY = getResources().getStringArray(R.array.list_category);
        String[] MOOD = getResources().getStringArray(R.array.list_mood);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CATEGORY);
        mCategorySpinner.setAdapter(categoryAdapter);
        mCategorySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCategory_id = position + 2;
                SavedData.setCategory_id(CameraPreviewActivity.this, mCategory_id);
            }
        });

        ArrayAdapter<String> moodAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, MOOD);
        mMoodSpinner.setAdapter(moodAdapter);
        mMoodSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mTag_id = position + 2;
                SavedData.setTag_id(CameraPreviewActivity.this, mTag_id);
            }
        });

        restAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, GocciCameraActivity.restname);
        mRestnameSpinner.setAdapter(restAdapter);
        mRestnameSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mRest_id = GocciCameraActivity.rest_id.get(position);
                mRestname = GocciCameraActivity.restname.get(position);
                SavedData.setRest_id(CameraPreviewActivity.this, mRest_id);
                SavedData.setRestname(CameraPreviewActivity.this, mRestname);
            }
        });

        mRestname = mRest_id == 1 ? "" : GocciCameraActivity.restname.get(GocciCameraActivity.rest_id.indexOf(mRest_id));

        mRestnameSpinner.setText(mRestname);
        mCategorySpinner.setText(mCategory_id == 1 ? "" : CATEGORY[mCategory_id - 2]);
        mMoodSpinner.setText(mTag_id == 1 ? "" : MOOD[mTag_id - 2]);
        mEditValue.setText(mValue);
        mEditComment.setText(mMemo);

        mPreviewVideo.setVideoPath(mVideoUrl);
        mPreviewVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                mp.setLooping(true);
            }
        });

        mToukouButtonRipple.setOnRippleCompleteListener(new RippleView.OnRippleCompleteListener() {
            @Override
            public void onComplete(RippleView rippleView) {
                if (Util.getConnectedState(CameraPreviewActivity.this) != Util.NetworkStatus.OFF) {
                    if (mRest_id != 1) {
                        if (mEditValue.getText().length() != 0) {
                            mValue = mEditValue.getText().toString();
                        } else {
                            mValue = "0";
                        }
                        SavedData.setValue(CameraPreviewActivity.this, mValue);
                        if (mEditComment.getText().length() != 0) {
                            mMemo = mEditComment.getText().toString();
                        } else {
                            mMemo = "none";
                        }
                        SavedData.setMemo(CameraPreviewActivity.this, mMemo);
                        if (mCheckCheer.isChecked()) {
                            mCheer_flag = 1;
                        }
                        postMovieBackground(CameraPreviewActivity.this);
                        postMovieAsync(CameraPreviewActivity.this);
                    } else {
                        Toast.makeText(CameraPreviewActivity.this, getString(R.string.please_input_restname), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CameraPreviewActivity.this, getString(R.string.bad_internet_connection), Toast.LENGTH_SHORT).show();
                }
            }
        });
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
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .content(getString(R.string.check_videoposting_cancel))
                .positiveText(getString(R.string.check_videoposting_yeah))
                .positiveColorRes(R.color.gocci_header)
                .negativeText(getString(R.string.check_videoposting_no))
                .negativeColorRes(R.color.gocci_header)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        CameraPreviewActivity.this.finish();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                    }
                }).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public final void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SavedData.setPostVideoPreview(this, mRestname, mRest_id, mVideoUrl, mAwsPostName, mCategory_id, mTag_id, mMemo, mValue, mIsnewRestname,
                mLongitude, mLatitude);
    }

    private void getTenpoJson(final Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(context, Const.getNearAPI(mLatitude, mLongitude), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                // Pull out the first event on the public timeline
                GocciCameraActivity.restname.clear();
                GocciCameraActivity.rest_id.clear();
                try {
                    for (int i = 0; i < timeline.length(); i++) {
                        JSONObject jsonObject = timeline.getJSONObject(i);

                        final String rest_name = jsonObject.getString("restname");
                        int rest_id = jsonObject.getInt("rest_id");

                        GocciCameraActivity.restname.add(rest_name);
                        GocciCameraActivity.rest_id.add(rest_id);
                    }
                    restAdapter.addAll(GocciCameraActivity.restname);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {

            }
        });
    }

    private void createTenpo() {
        new MaterialDialog.Builder(CameraPreviewActivity.this)
                .content(getString(R.string.add_restname))
                .input(getString(R.string.restname), null, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                        materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(charSequence.length() > 0);
                    }
                })
                .widgetColorRes(R.color.gocci_header)
                .alwaysCallInputCallback()
                .positiveText(getString(R.string.add_restname_post))
                .positiveColorRes(R.color.gocci_header)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        mRestname = dialog.getInputEditText().getText().toString();

                        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(CameraPreviewActivity.this));
                        Const.asyncHttpClient.get(CameraPreviewActivity.this, Const.getPostRestAddAPI(mRestname, mLatitude, mLongitude), new JsonHttpResponseHandler() {
                            @Override
                            public void onStart() {
                                mProgressWheel.setVisibility(View.VISIBLE);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                Toast.makeText(CameraPreviewActivity.this, getString(R.string.error_internet_connection), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    String message = response.getString("message");

                                    if (message.equals(getString(R.string.add_restname_complete_message))) {
                                        Toast.makeText(CameraPreviewActivity.this, message, Toast.LENGTH_SHORT).show();
                                        //店名をセット
                                        mIsnewRestname = true;
                                        mRestnameSpinner.setText(mRestname);
                                        mRest_id = response.getInt("rest_id");
                                        mRestnameSpinner.setClickable(false);
                                        SavedData.setIsNewRestname(CameraPreviewActivity.this, mIsnewRestname);
                                        SavedData.setRestname(CameraPreviewActivity.this, mRestname);
                                        SavedData.setRest_id(CameraPreviewActivity.this, mRest_id);
                                    } else {
                                        Toast.makeText(CameraPreviewActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }

                            @Override
                            public void onFinish() {
                                mProgressWheel.setVisibility(View.INVISIBLE);
                            }
                        });
                    }
                })
                .show();
    }

    private void postMovieBackground(Context context) {
        TransferObserver transferObserver = Application_Gocci.getTransfer(context).upload(Const.POST_MOVIE_BUCKET_NAME, mAwsPostName + ".mp4", mVideoFile);
        transferObserver.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    isError = false;
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {

            }

            @Override
            public void onError(int id, Exception ex) {
                isError = true;
                Toast.makeText(CameraPreviewActivity.this, getString(R.string.bad_internet_connection), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void postMovieAsync(final Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.setConnectTimeout(10 * 1000);
        Const.asyncHttpClient.setResponseTimeout(60 * 1000);
        Const.asyncHttpClient.get(context, Const.getPostMovieAPI(mRest_id, mAwsPostName, mCategory_id, mTag_id, mValue, mMemo, mCheer_flag), new JsonHttpResponseHandler() {
            @Override
            public void onStart() {
                mProgressWheel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(context, getString(R.string.videoposting_failure), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    String message = response.getString("message");
                    int code = response.getInt("code");

                    if (code == 200 && message.equals(getString(R.string.videoposting_success))) {
                        Toast.makeText(context, getString(R.string.videoposting_message), Toast.LENGTH_SHORT).show();

                        SharedPreferences prefs = context.getSharedPreferences("movie", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.clear();
                        editor.apply();

                        Intent intent = new Intent(context, GocciTimelineActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(context, getString(R.string.videoposting_failure), Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFinish() {
                mProgressWheel.setVisibility(View.GONE);
            }
        });
    }
}