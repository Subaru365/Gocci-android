package com.inase.android.gocci.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.andexert.library.RippleView;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;
import com.inase.android.gocci.Application_Gocci;
import com.inase.android.gocci.R;
import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.ui.view.SquareVideoView;
import com.inase.android.gocci.utils.SavedData;
import com.inase.android.gocci.utils.Util;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.pnikosis.materialishprogress.ProgressWheel;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;
import io.fabric.sdk.android.Fabric;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class CameraPreviewAlreadyExistActivity extends AppCompatActivity {

    @Bind(R.id.preview_video)
    SquareVideoView mPreviewVideo;
    @Bind(R.id.restname_spinner)
    MaterialBetterSpinner mRestnameSpinner;
    @Bind(R.id.category_spinner)
    MaterialBetterSpinner mCategorySpinner;
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
    @Bind(R.id.add_rest_text)
    TextView mAddRestText;

    @OnClick(R.id.add_rest_text)
    public void restAdd() {
        createTenpo();
    }

    @OnClick(R.id.button_twitter)
    public void twitter() {
        Uri bmpUri = Util.getUri(mVideoUrl);
        if (bmpUri != null) {
            if (mRestname.equals("")) {
                Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.please_input_restname), Toast.LENGTH_SHORT).show();
            } else {
                TweetComposer.Builder builder = new TweetComposer.Builder(CameraPreviewAlreadyExistActivity.this)
                        .text("#" + mRestname.replaceAll("\\s+", "") + " #Gocci")
                        .image(bmpUri);

                builder.show();
            }
        } else {
            // ...sharing failed, handle error
            Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.error_share), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.error_share), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.please_input_restname), Toast.LENGTH_SHORT).show();
        }
    }

    private String mRest_id;
    private int mCategory_id;
    private int mCheer_flag = 0;
    private String mRestname;
    private String mVideoUrl;
    private String mAwsPostName;
    private String mValue;
    private String mMemo;
    private boolean mIsnewRestname;
    private double mLatitude;
    private double mLongitude;

    private ArrayList<String> rest_nameList = new ArrayList<>();
    private ArrayList<String> rest_idList = new ArrayList<>();

    private File mVideoFile;

    private CallbackManager callbackManager;
    private ShareDialog shareDialog;

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

        setContentView(R.layout.activity_camera_preview_already_exist);
        ButterKnife.bind(this);

        mRest_id = SavedData.getRest_id(this);
        mRestname = SavedData.getRestname(this);
        mVideoUrl = SavedData.getVideoUrl(this);
        mAwsPostName = SavedData.getAwsPostname(this);
        mCategory_id = SavedData.getCategory_id(this);
        mMemo = SavedData.getMemo(this);
        mValue = SavedData.getValue(this);
        mIsnewRestname = SavedData.getIsNewRestname(this);
        mLatitude = SavedData.getLat(this);
        mLongitude = SavedData.getLon(this);

        mVideoFile = new File(mVideoUrl);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.complete_share), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.cancel_share), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.error_share), Toast.LENGTH_SHORT).show();
            }
        });

        Fabric.with(this, new TweetComposer());

        String[] CATEGORY = getResources().getStringArray(R.array.list_category);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CATEGORY);
        mCategorySpinner.setAdapter(categoryAdapter);
        mCategorySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mCategory_id = position + 2;
                SavedData.setCategory_id(CameraPreviewAlreadyExistActivity.this, mCategory_id);
            }
        });

        restAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, rest_nameList);
        mRestnameSpinner.setAdapter(restAdapter);
        mRestnameSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mRest_id = rest_idList.get(position);
                mRestname = rest_nameList.get(position);
                SavedData.setRest_id(CameraPreviewAlreadyExistActivity.this, mRest_id);
                SavedData.setRestname(CameraPreviewAlreadyExistActivity.this, mRestname);
            }
        });

        if (!mRestname.equals("")) {
            mRestnameSpinner.setClickable(false);
        } else {
            if (mLatitude == 0.0 && mLongitude == 0.0) {
                SmartLocation.with(this).location().oneFix().start(new OnLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        mLatitude = location.getLatitude();
                        mLongitude = location.getLongitude();
                        getTenpoJson(CameraPreviewAlreadyExistActivity.this);
                        SavedData.setLat(CameraPreviewAlreadyExistActivity.this, mLatitude);
                        SavedData.setLon(CameraPreviewAlreadyExistActivity.this, mLongitude);
                    }
                });
            } else {
                getTenpoJson(this);
            }
        }

        mRestnameSpinner.setText(mRestname);
        mCategorySpinner.setText(mCategory_id == 1 ? "" : CATEGORY[mCategory_id - 2]);
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
                if (Util.getConnectedState(CameraPreviewAlreadyExistActivity.this) != Util.NetworkStatus.OFF) {
                    if (!mRest_id.equals("1")) {
                        if (mEditValue.getText().length() != 0) {
                            mValue = mEditValue.getText().toString();
                        } else {
                            mValue = "0";
                        }
                        SavedData.setValue(CameraPreviewAlreadyExistActivity.this, mValue);
                        if (mEditComment.getText().length() != 0) {
                            mMemo = mEditComment.getText().toString();
                        } else {
                            mMemo = "none";
                        }
                        SavedData.setMemo(CameraPreviewAlreadyExistActivity.this, mMemo);
                        if (mCheckCheer.isChecked()) {
                            mCheer_flag = 1;
                        }
                        postMovieAsync(CameraPreviewAlreadyExistActivity.this);
                        Application_Gocci.postingVideoToS3(CameraPreviewAlreadyExistActivity.this, mAwsPostName, mVideoFile);
                    } else {
                        Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.please_input_restname), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.bad_internet_connection), Toast.LENGTH_SHORT).show();
                }
            }
        });

        if (mIsnewRestname || !mRestname.equals("")) {
            mAddRestText.setVisibility(View.GONE);
        }
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
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public final void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SavedData.setPostVideoPreview(this, mRestname, mRest_id, mVideoUrl, mAwsPostName, mCategory_id, mMemo, mValue, mIsnewRestname,
                mLongitude, mLatitude);
    }

    private void getTenpoJson(final Context context) {
        Application_Gocci.getJsonAsyncHttpClient(Const.getNearAPI(mLatitude, mLongitude), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                // Pull out the first event on the public timeline
                try {
                    for (int i = 0; i < timeline.length(); i++) {
                        JSONObject jsonObject = timeline.getJSONObject(i);

                        final String rest_name = jsonObject.getString("restname");
                        String rest_id = jsonObject.getString("rest_id");

                        rest_nameList.add(rest_name);
                        rest_idList.add(rest_id);
                    }
                    restAdapter.addAll(rest_nameList);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                Toast.makeText(context, getString(R.string.error_internet_connection), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createTenpo() {
        new MaterialDialog.Builder(CameraPreviewAlreadyExistActivity.this)
                .content(getString(R.string.add_restname))
                .contentColorRes(R.color.nameblack)
                .inputType(InputType.TYPE_CLASS_TEXT)
                .widgetColorRes(R.color.nameblack)
                .positiveText("送信")
                .positiveColorRes(R.color.gocci_header)
                .input("", "", false, new MaterialDialog.InputCallback() {
                    @Override
                    public void onInput(MaterialDialog materialDialog, CharSequence charSequence) {
                        mRestname = charSequence.toString();
                        Application_Gocci.getJsonAsyncHttpClient(Const.getPostRestAddAPI(mRestname, mLatitude, mLongitude), new JsonHttpResponseHandler() {

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                Toast.makeText(CameraPreviewAlreadyExistActivity.this, getString(R.string.error_internet_connection), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                try {
                                    String message = response.getString("message");

                                    if (message.equals(getString(R.string.add_restname_complete_message))) {
                                        Toast.makeText(CameraPreviewAlreadyExistActivity.this, message, Toast.LENGTH_SHORT).show();
                                        //店名をセット
                                        mIsnewRestname = true;
                                        mRestnameSpinner.setText(mRestname);
                                        mRest_id = response.getString("rest_id");
                                        mRestnameSpinner.setClickable(false);
                                        SavedData.setIsNewRestname(CameraPreviewAlreadyExistActivity.this, mIsnewRestname);
                                        SavedData.setRestname(CameraPreviewAlreadyExistActivity.this, mRestname);
                                        SavedData.setRest_id(CameraPreviewAlreadyExistActivity.this, mRest_id);
                                    } else {
                                        Toast.makeText(CameraPreviewAlreadyExistActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }).show();
    }

    private void postMovieAsync(final Context context) {
        Application_Gocci.getJsonAsyncHttpClient(Const.getPostMovieAPI(mRest_id, mAwsPostName, mCategory_id, mValue, mMemo, mCheer_flag), new JsonHttpResponseHandler() {

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

                        Intent intent = new Intent(context, TimelineActivity.class);
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
