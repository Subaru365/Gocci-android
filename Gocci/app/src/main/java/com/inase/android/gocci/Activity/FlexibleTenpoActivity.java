package com.inase.android.gocci.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.andexert.library.RippleView;
import com.cocosw.bottomsheet.BottomSheet;
import com.coremedia.iso.boxes.Container;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.share.Sharer;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;
import com.github.ksoichiro.android.observablescrollview.ObservableRecyclerView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.google.android.exoplayer.audio.AudioCapabilities;
import com.google.android.exoplayer.audio.AudioCapabilitiesReceiver;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.inase.android.gocci.Application.Application_Gocci;
import com.inase.android.gocci.Base.RoundedTransformation;
import com.inase.android.gocci.Event.BusHolder;
import com.inase.android.gocci.Event.NotificationNumberEvent;
import com.inase.android.gocci.R;
import com.inase.android.gocci.VideoPlayer.HlsRendererBuilder;
import com.inase.android.gocci.VideoPlayer.VideoPlayer;
import com.inase.android.gocci.View.DrawerProfHeader;
import com.inase.android.gocci.common.CacheManager;
import com.inase.android.gocci.common.Const;
import com.inase.android.gocci.common.SavedData;
import com.inase.android.gocci.common.Util;
import com.inase.android.gocci.data.HeaderData;
import com.inase.android.gocci.data.PostData;
import com.loopj.android.http.TextHttpResponseHandler;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.fabric.sdk.android.Fabric;

public class FlexibleTenpoActivity extends AppCompatActivity implements AudioCapabilitiesReceiver.Listener, ObservableScrollViewCallbacks, AppBarLayout.OnOffsetChangedListener {

    private String mTenpoUrl;

    private MapView mapView;

    private int mWant_flag;
    private int mTotal_cheer_num;
    private TextView cheer_number;

    private ArrayList<PostData> mTenpousers = new ArrayList<PostData>();
    private TenpoAdapter mTenpoAdapter;
    private ObservableRecyclerView mTenpoRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private HeaderData headerTenpoData;

    private int mRest_id;

    private AttributeSet mVideoAttr;
    private Point mDisplaySize;
    private CacheManager mCacheManager;
    private String mPlayingPostId;
    private boolean mPlayBlockFlag;
    private ConcurrentHashMap<Const.ExoViewHolder, String> mViewHolderHash;  // Value: PosterId

    private boolean isExist = false;
    private boolean isSee = false;

    private CallbackManager callbackManager;
    private ShareDialog shareDialog;

    private int previousTotal = 0;
    private boolean loading = true;
    private int visibleThreshold = 5;
    int firstVisibleItem, visibleItemCount, totalItemCount;

    private String addUrl;

    private Drawer result;

    private final FlexibleTenpoActivity self = this;

    private CollapsingToolbarLayout collapsingToolbarLayout;

    private AppBarLayout appBarLayout;

    private VideoPlayer player;
    private boolean playerNeedsPrepare;

    private long playerPosition;

    private AudioCapabilitiesReceiver audioCapabilitiesReceiver;
    private AudioCapabilities audioCapabilities;

    private ViewTreeObserver.OnGlobalLayoutListener mOnGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            Log.e("DEBUG", "onGlobalLayout called: " + mPlayingPostId);
            if (isSee) {
                changeMovie();
            }
            Log.e("DEBUG", "onGlobalLayout  changeMovie called: " + mPlayingPostId);
            if (mPlayingPostId != null || !isExist) {
                mTenpoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        }
    };

    private static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            FlexibleTenpoActivity activity
                    = (FlexibleTenpoActivity) msg.obj;
            switch (msg.what) {
                case Const.INTENT_TO_TIMELINE:
                    activity.startActivity(new Intent(activity, GocciTimelineActivity.class));
                    activity.overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                    break;
                case Const.INTENT_TO_MYPAGE:
                    GocciMyprofActivity.startMyProfActivity(activity);
                    break;
                case Const.INTENT_TO_USERPAGE:
                    FlexibleUserProfActivity.startUserProfActivity(msg.arg1, activity);
                    break;
                case Const.INTENT_TO_COMMENT:
                    CommentActivity.startCommentActivity(msg.arg1, activity);
                    break;
                case Const.INTENT_TO_POLICY:
                    WebViewActivity.startWebViewActivity(1, activity);
                    break;
                case Const.INTENT_TO_LICENSE:
                    WebViewActivity.startWebViewActivity(2, activity);
                    break;
                case Const.INTENT_TO_ADVICE:
                    Util.setAdviceDialog(activity);
                    break;
                case Const.INTENT_TO_LIST:
                    ListActivity.startListActivity(msg.arg1, 0, msg.arg2, activity);
                    break;
            }
        }
    };

    public static void startTenpoActivity(int rest_id, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, FlexibleTenpoActivity.class);
        intent.putExtra("rest_id", rest_id);
        startingActivity.startActivity(intent);
        startingActivity.overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    @Override
    public final void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCacheManager = CacheManager.getInstance(getApplicationContext());
        audioCapabilitiesReceiver = new AudioCapabilitiesReceiver(getApplicationContext(), this);
        // 画面回転に対応するならonResumeが安全かも
        mDisplaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(mDisplaySize);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        shareDialog = new ShareDialog(this);
        shareDialog.registerCallback(callbackManager, new FacebookCallback<Sharer.Result>() {
            @Override
            public void onSuccess(Sharer.Result result) {
                Toast.makeText(FlexibleTenpoActivity.this, "シェアが完了しました", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                Toast.makeText(FlexibleTenpoActivity.this, "キャンセルしました", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(FlexibleTenpoActivity.this, "シェアに失敗しました", Toast.LENGTH_SHORT).show();
            }
        });

        Fabric.with(this, new TweetComposer());

        mPlayBlockFlag = false;

        // 初期化処理
        mPlayingPostId = null;
        mViewHolderHash = new ConcurrentHashMap<>();

        setContentView(R.layout.activity_flexible_tenpo);

        Intent intent = getIntent();
        mRest_id = intent.getIntExtra("rest_id", 0);

        mTenpoUrl = Const.getRestpageAPI(mRest_id);

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        //toolbar.inflateMenu(R.menu.toolbar_menu);
        //toolbar.setLogo(R.drawable.ic_gocci_moji_white45);
        setSupportActionBar(toolbar);

        result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(toolbar)
                .withHeader(new DrawerProfHeader(this))
                .addDrawerItems(
                        new PrimaryDrawerItem().withName("タイムライン").withIcon(GoogleMaterial.Icon.gmd_home).withIdentifier(1).withCheckable(false),
                        new PrimaryDrawerItem().withName("マイページ").withIcon(GoogleMaterial.Icon.gmd_person).withIdentifier(2).withCheckable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName("要望を送る").withIcon(GoogleMaterial.Icon.gmd_send).withCheckable(false).withIdentifier(3),
                        new PrimaryDrawerItem().withName("利用規約とポリシー").withIcon(GoogleMaterial.Icon.gmd_visibility).withCheckable(false).withIdentifier(4),
                        new PrimaryDrawerItem().withName("ライセンス情報").withIcon(GoogleMaterial.Icon.gmd_build).withCheckable(false).withIdentifier(5)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        // do something with the clicked item :D
                        if (drawerItem != null) {
                            if (drawerItem.getIdentifier() == 1) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_TIMELINE, 0, 0, FlexibleTenpoActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 2) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_MYPAGE, 0, 0, FlexibleTenpoActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 3) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_ADVICE, 0, 0, FlexibleTenpoActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 4) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_POLICY, 0, 0, FlexibleTenpoActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 5) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_LICENSE, 0, 0, FlexibleTenpoActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            }
                        }
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .withSelectedItem(-1)
                .withOnDrawerNavigationListener(new Drawer.OnDrawerNavigationListener() {
                    @Override
                    public boolean onNavigationClickListener(View view) {
                        finish();
                        overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
                        return true;
                    }
                })
                .build();

        result.getActionBarDrawerToggle().setDrawerIndicatorEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);

        cheer_number = (TextView) findViewById(R.id.cheer_number);
        cheer_number.setText(String.valueOf(mTotal_cheer_num));
        mTenpoRecyclerView = (ObservableRecyclerView) findViewById(R.id.list);
        mLayoutManager = new LinearLayoutManager(this);
        mTenpoRecyclerView.setLayoutManager(mLayoutManager);
        mTenpoRecyclerView.setHasFixedSize(true);
        mTenpoRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        mTenpoRecyclerView.setScrollViewCallbacks(this);
        mTenpoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    // スクロールしていない
                    case RecyclerView.SCROLL_STATE_IDLE:
                        //mBusy = false;
                        Log.d("DEBUG", "SCROLL_STATE_IDLE");
                        if (isSee) {
                            changeMovie();
                        }
                        break;
                    // スクロール中
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        //mBusy = true;
                        Log.d("DEBUG", "SCROLL_STATE_DRAGGING");
                        break;
                    // はじいたとき
                    case RecyclerView.SCROLL_STATE_SETTLING:
                        //mBusy = true;
                        Log.d("DEBUG", "SCROLL_STATE_SETTLING");
                        break;
                }

                visibleItemCount = mTenpoRecyclerView.getChildCount();
                totalItemCount = mLayoutManager.getItemCount();
                firstVisibleItem = mLayoutManager.findFirstVisibleItemPosition();

                if (totalItemCount != 1) {
                    //投稿はある
                    isExist = true;
                } else {
                    //投稿がない
                    isExist = false;
                }

                if (loading) {
                    if (totalItemCount > previousTotal) {
                        loading = false;
                        previousTotal = totalItemCount;
                    }
                }
                if (!loading && (totalItemCount - visibleItemCount)
                        <= (firstVisibleItem + visibleThreshold)) {
                    // End has been reached

                    loading = true;
                }
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                if (Util.getConnectedState(FlexibleTenpoActivity.this) != Util.NetworkStatus.OFF) {
                    getRefreshAsync(FlexibleTenpoActivity.this);
                } else {
                    Toast.makeText(FlexibleTenpoActivity.this, "通信に失敗しました", Toast.LENGTH_LONG).show();
                    mSwipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        appBarLayout = (AppBarLayout) findViewById(R.id.appbar);

        getSignupAsync(FlexibleTenpoActivity.this);
    }

    @Override
    public final void onDestroy() {
        if (mapView != null) {
            mapView.onDestroy();
        }
        releasePlayer();
        super.onDestroy();

    }

    @Override
    public final void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public final void onPause() {
        if (mapView != null) {
            mapView.onPause();
        }
        super.onPause();
        BusHolder.get().unregister(self);

        if (player != null) {
            player.blockingClearSurface();
        }
        releasePlayer();
        audioCapabilitiesReceiver.unregister();
        if (getPlayingViewHolder() != null) {
            getPlayingViewHolder().mVideoThumbnail.setVisibility(View.VISIBLE);
        }
        appBarLayout.removeOnOffsetChangedListener(this);

    }

    @Override
    public final void onResume() {
        super.onResume();
        BusHolder.get().register(self);
        if (mapView != null) {
            mapView.onResume();
        }

        audioCapabilitiesReceiver.register();
        appBarLayout.addOnOffsetChangedListener(this);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onAudioCapabilitiesChanged(AudioCapabilities audioCapabilities) {
        boolean audioCapabilitiesChanged = !audioCapabilities.equals(this.audioCapabilities);
        if (player == null || audioCapabilitiesChanged) {
            if (mPlayingPostId != null && isSee) {
                this.audioCapabilities = audioCapabilities;
                releasePlayer();
                preparePlayer(getPlayingViewHolder(), getVideoPath());
            }
        } else {
            player.setBackgrounded(false);
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
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.activity_back_in, R.anim.activity_back_out);
        }
    }

    @Subscribe
    public void subscribe(NotificationNumberEvent event) {
        android.support.design.widget.Snackbar.make(mTenpoRecyclerView, event.mMessage, android.support.design.widget.Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public final void onSaveInstanceState(Bundle outState) {
        outState = result.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    private void getSignupAsync(final Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(context, mTenpoUrl, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                try {
                    JSONObject jsonObject = new JSONObject(responseString);
                    JSONObject headerObject = jsonObject.getJSONObject("restaurants");
                    JSONArray postsObject = jsonObject.getJSONArray("posts");

                    headerTenpoData = HeaderData.createTenpoHeaderData(headerObject);

                    for (int i = 0; i < postsObject.length(); i++) {
                        JSONObject post = postsObject.getJSONObject(i);
                        mTenpousers.add(PostData.createPostData(post));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                collapsingToolbarLayout.setTitle(headerTenpoData.getRestname());
                mTenpoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
                mTenpoAdapter = new TenpoAdapter(FlexibleTenpoActivity.this);
                mTenpoRecyclerView.setAdapter(mTenpoAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast.makeText(FlexibleTenpoActivity.this, "読み取りに失敗しました", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                Log.d("DEBUG", "ProgressDialog dismiss getTimeline finish");
            }
        });
    }

    private void getRefreshAsync(final Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(context, mTenpoUrl, new TextHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                mTenpousers.clear();
                try {
                    JSONObject jsonObject = new JSONObject(responseString);
                    JSONObject headerObject = jsonObject.getJSONObject("restaurants");
                    JSONArray postsObject = jsonObject.getJSONArray("posts");

                    headerTenpoData = HeaderData.createTenpoHeaderData(headerObject);

                    for (int i = 0; i < postsObject.length(); i++) {
                        JSONObject post = postsObject.getJSONObject(i);
                        mTenpousers.add(PostData.createPostData(post));
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mPlayingPostId = null;
                mViewHolderHash.clear();
                mTenpoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mOnGlobalLayoutListener);
                mTenpoAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                Toast.makeText(FlexibleTenpoActivity.this, "読み取りに失敗しました", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFinish() {
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private String getVideoPath() {
        final int position = mTenpoRecyclerView.getChildAdapterPosition(mTenpoRecyclerView.findChildViewUnder(mDisplaySize.x / 2, mDisplaySize.y / 2));
        final PostData userData = mTenpoAdapter.getItem(position - 1);
        if (!userData.getPost_id().equals(mPlayingPostId)) {
            return null;
        }
        //return mCacheManager.getCachePath(userData.getPost_id(), userData.getMovie());
        return userData.getMovie();
    }

    private void preparePlayer(final Const.ExoViewHolder viewHolder, String path) {
        if (player == null) {
            player = new VideoPlayer(new HlsRendererBuilder(this, com.google.android.exoplayer.util.Util.getUserAgent(this, "Gocci"), path,
                    audioCapabilities));
            player.addListener(new VideoPlayer.Listener() {
                @Override
                public void onStateChanged(boolean playWhenReady, int playbackState) {
                    switch (playbackState) {
                        case VideoPlayer.STATE_BUFFERING:
                            break;
                        case VideoPlayer.STATE_ENDED:
                            player.seekTo(0);
                            break;
                        case VideoPlayer.STATE_IDLE:
                            break;
                        case VideoPlayer.STATE_PREPARING:
                            break;
                        case VideoPlayer.STATE_READY:
                            break;
                        default:
                            break;
                    }
                }

                @Override
                public void onError(Exception e) {
                    playerNeedsPrepare = true;
                }

                @Override
                public void onVideoSizeChanged(int width, int height, float pixelWidthAspectRatio) {
                    viewHolder.mVideoThumbnail.setVisibility(View.GONE);
                    viewHolder.videoFrame.setAspectRatio(
                            height == 0 ? 1 : (width * pixelWidthAspectRatio) / height);
                }
            });
            //player.seekTo(playerPosition);
            playerNeedsPrepare = true;
        }
        if (playerNeedsPrepare) {
            player.prepare();
            playerNeedsPrepare = false;
        }
        player.setSurface(viewHolder.movie.getHolder().getSurface());
        player.setPlayWhenReady(true);
    }

    private void releasePlayer() {
        if (player != null) {
            //playerPosition = player.getCurrentPosition();
            player.release();
            player = null;
        }
    }

    private void changeMovie() {
        Log.e("DEBUG", "changeMovie called");
        // TODO:実装
        final int position = mTenpoRecyclerView.getChildAdapterPosition(mTenpoRecyclerView.findChildViewUnder(mDisplaySize.x / 2, mDisplaySize.y / 2));
        if (mTenpoAdapter.isEmpty()) {
            return;
        }
        if (position - 1 < 0) {
            return;
        }

        final PostData userData = mTenpoAdapter.getItem(position - 1);
        if (!userData.getPost_id().equals(mPlayingPostId)) {
            Log.d("DEBUG", "postId change");

            mPlayingPostId = userData.getPost_id();
            final Const.ExoViewHolder currentViewHolder = getPlayingViewHolder();
            Log.d("DEBUG", "MOVIE::changeMovie 動画再生処理開始 postId:" + mPlayingPostId);
            if (mPlayBlockFlag) {
                Log.d("DEBUG", "startMovie play block status");
                return;
            }

            final String path = userData.getMovie();
            Log.e("DEBUG", "[ProgressBar GONE] cache Path: " + path);
            releasePlayer();
            preparePlayer(currentViewHolder, path);
        }
    }

    /**
     * 現在再生中のViewHolderを取得
     *
     * @return
     */
    private Const.ExoViewHolder getPlayingViewHolder() {
        Const.ExoViewHolder viewHolder = null;
        Log.d("DEBUG", "getPlayingViewHolder :" + mPlayingPostId);
        if (mPlayingPostId != null) {
            for (Map.Entry<Const.ExoViewHolder, String> entry : mViewHolderHash.entrySet()) {
                if (entry.getValue().equals(mPlayingPostId)) {
                    viewHolder = entry.getKey();
                    break;
                }
            }
        }
        return viewHolder;
    }

    @Override
    public void onScrollChanged(int i, boolean b, boolean b1) {
        //ヘッダー通り過ぎた
        Log.e("スクロール", String.valueOf(i));
        isSee = i > 500;
    }

    @Override
    public void onDownMotionEvent() {

    }

    @Override
    public void onUpOrCancelMotionEvent(ScrollState scrollState) {

    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        if (i == 0) {
            mSwipeRefreshLayout.setEnabled(true);
        } else {
            mSwipeRefreshLayout.setEnabled(false);
        }
    }

    public class TenpoHeaderViewHolder extends RecyclerView.ViewHolder implements OnMapReadyCallback {
        private TextView tenpo_category;
        private RippleView checkRipple;
        private ImageView check_Image;
        private TextView check_text;
        private RippleView callRipple;
        private RippleView gohereRipple;
        private RippleView etcRipple;
        private MapView mMapView;
        private GoogleMap mGoogleMap;

        public TenpoHeaderViewHolder(View view) {
            super(view);
            tenpo_category = (TextView) view.findViewById(R.id.category);
            checkRipple = (RippleView) view.findViewById(R.id.checkRipple);
            check_Image = (ImageView) view.findViewById(R.id.check_image);
            check_text = (TextView) view.findViewById(R.id.check_text);
            callRipple = (RippleView) view.findViewById(R.id.callRipple);
            gohereRipple = (RippleView) view.findViewById(R.id.gohereRipple);
            etcRipple = (RippleView) view.findViewById(R.id.etcRipple);
            mMapView = (MapView) view.findViewById(R.id.map);

            if (mMapView != null) {
                mMapView.onCreate(null);
                mMapView.onResume();
                mapView = mMapView;
                mMapView.getMapAsync(this);
            }
        }

        @Override
        public void onMapReady(GoogleMap googleMap) {
            MapsInitializer.initialize(getApplicationContext());
            mGoogleMap = googleMap;

            LatLng lng = new LatLng(headerTenpoData.getLat(), headerTenpoData.getLon());
            mGoogleMap.getUiSettings().setCompassEnabled(false);
            mGoogleMap.addMarker(new MarkerOptions().position(lng).title(headerTenpoData.getRestname()));
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(lng)
                    .zoom(15)
                    .tilt(50)
                    .build();
            mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
    }

    public class TenpoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        public static final int TYPE_TENPO_HEADER = 0;
        public static final int TYPE_POST = 1;

        private Context context;

        public TenpoAdapter(Context context) {
            this.context = context;
        }

        public PostData getItem(int position) {
            return mTenpousers.get(position);
        }

        public boolean isEmpty() {
            return mTenpousers.isEmpty();
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) {
                return TYPE_TENPO_HEADER;
            } else {
                return TYPE_POST;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (TYPE_TENPO_HEADER == viewType) {
                final View view = LayoutInflater.from(context).inflate(R.layout.cell_tenpo_header, parent, false);
                return new TenpoHeaderViewHolder(view);
            } else {
                final View view = LayoutInflater.from(context).inflate(R.layout.cell_exo_timeline, parent, false);
                return new Const.ExoViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            int viewType = getItemViewType(position);
            if (TYPE_TENPO_HEADER == viewType) {
                bindHeader((TenpoHeaderViewHolder) holder);
            } else {
                PostData users = mTenpousers.get(position - 1);
                bindPost((Const.ExoViewHolder) holder, position - 1, users);
            }
        }

        private void bindHeader(final TenpoHeaderViewHolder holder) {
            if (headerTenpoData.getWant_flag() == 0) {
                holder.check_Image.setImageResource(R.drawable.ic_like_white);
                holder.check_text.setText("行きたい店に認定");
            } else {
                holder.check_Image.setImageResource(R.drawable.ic_favorite_orange);
                holder.check_text.setText("行きたい店を取消");
            }

            holder.checkRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (holder.check_text.getText().toString().equals("行きたい店に認定")) {
                        holder.check_Image.setImageResource(R.drawable.ic_favorite_orange);
                        holder.check_text.setText("行きたい店を取消");

                        Util.wantAsync(context, headerTenpoData);
                    } else {
                        holder.check_Image.setImageResource(R.drawable.ic_like_white);
                        holder.check_text.setText("行きたい店に認定");

                        Util.unwantAsync(context, headerTenpoData);
                    }
                }
            });
            holder.callRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Handler handler = new Handler();
                    handler.postDelayed(new callClickHandler(), 750);
                }
            });
            holder.gohereRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Handler handler = new Handler();
                    handler.postDelayed(new gohereClickHandler(), 750);
                }
            });
            holder.etcRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!headerTenpoData.getHomepage().equals("none")) {
                        new MaterialDialog.Builder(FlexibleTenpoActivity.this)
                                .title("その他メニュー")
                                .items(R.array.list_tenpo_menu)
                                .itemsCallback(new com.afollestad.materialdialogs.MaterialDialog.ListCallback() {
                                    @Override
                                    public void onSelection(com.afollestad.materialdialogs.MaterialDialog materialDialog, View view, int i, CharSequence charSequence) {
                                        if (charSequence.toString().equals("ホームページを見る")) {
                                            Uri uri = Uri.parse(headerTenpoData.getHomepage());
                                            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                                            startActivity(intent);
                                            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                                        }
                                    }
                                })
                                .show();
                    } else {
                        Toast.makeText(FlexibleTenpoActivity.this, "その他メニューはありません", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            if (holder.mGoogleMap == null) {
                holder.mGoogleMap = holder.mMapView.getMap();
            }
            if (holder.mGoogleMap != null) {
                //move map to the 'location'
                LatLng lng = new LatLng(headerTenpoData.getLat(), headerTenpoData.getLon());
                holder.mGoogleMap.getUiSettings().setCompassEnabled(false);
                holder.mGoogleMap.addMarker(new MarkerOptions().position(lng).title(headerTenpoData.getRestname()));
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(lng)
                        .zoom(15)
                        .tilt(50)
                        .build();
                holder.mGoogleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }

            holder.tenpo_category.setText(headerTenpoData.getRest_category());
        }

        private void bindPost(final Const.ExoViewHolder holder, final int position, final PostData user) {
            holder.user_name.setText(user.getUsername());

            holder.datetime.setText(user.getPost_date());

            holder.comment.setText(user.getMemo());

            Picasso.with(FlexibleTenpoActivity.this)
                    .load(user.getProfile_img())
                    .placeholder(R.drawable.ic_userpicture)
                    .transform(new RoundedTransformation())
                    .into(holder.circleImage);

            holder.user_name.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_USERPAGE, user.getPost_user_id(), user.getPost_user_id(), FlexibleTenpoActivity.this);
                    sHandler.sendMessageDelayed(msg, 750);
                }
            });

            holder.circleImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_USERPAGE, user.getPost_user_id(), user.getPost_user_id(), FlexibleTenpoActivity.this);
                    sHandler.sendMessageDelayed(msg, 750);
                }
            });

            holder.menuRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new BottomSheet.Builder(FlexibleTenpoActivity.this, R.style.BottomSheet_StyleDialog).sheet(R.menu.popup_normal).listener(new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case R.id.violation:
                                    Util.setViolateDialog(FlexibleTenpoActivity.this, user.getPost_id());
                                    break;
                                case R.id.close:
                                    dialog.dismiss();
                            }
                        }
                    }).show();
                }
            });
            Picasso.with(context)
                    .load(user.getThumbnail())
                    .placeholder(R.color.videobackground)
                    .into(holder.mVideoThumbnail);
            holder.mVideoThumbnail.setVisibility(View.VISIBLE);

            holder.videoFrame.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (player != null) {
                        if (player.getPlayerControl().isPlaying()) {
                            player.getPlayerControl().pause();
                        } else {
                            player.getPlayerControl().start();
                            holder.mVideoThumbnail.setVisibility(View.INVISIBLE);
                        }
                    } else {
                        releasePlayer();
                        preparePlayer(holder, user.getMovie());
                    }
                }
            });


            holder.rest_name.setText(user.getRestname());
            //viewHolder.locality.setText(user.getLocality());

            if (!user.getCategory().equals("none")) {
                holder.category.setText(user.getCategory());
            } else {
                holder.category.setText("　　　　");
            }
            if (!user.getTag().equals("none")) {
                holder.atmosphere.setText(user.getTag());
            } else {
                holder.atmosphere.setText("　　　　");
            }
            if (!user.getValue().equals("0")) {
                holder.value.setText(user.getValue() + "円");
            } else {
                holder.value.setText("　　　　");
            }

            final int currentgoodnum = user.getGochi_num();
            final int currentcommentnum = user.getComment_num();

            holder.likes.setText(String.valueOf(currentgoodnum));
            holder.comments.setText(String.valueOf(currentcommentnum));

            if (user.getGochi_flag() == 0) {
                holder.likes_ripple.setClickable(true);
                holder.likes_Image.setImageResource(R.drawable.ic_icon_beef);

                holder.likes_ripple.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e("いいねをクリック", user.getPost_id());
                        user.setGochi_flag(1);
                        user.setGochi_num(currentgoodnum + 1);

                        holder.likes.setText(String.valueOf((currentgoodnum + 1)));
                        holder.likes_Image.setImageResource(R.drawable.ic_icon_beef_orange);
                        holder.likes_ripple.setClickable(false);

                        Util.postGochiAsync(context, user);
                    }
                });
            } else {
                holder.likes_Image.setImageResource(R.drawable.ic_icon_beef_orange);
                holder.likes_ripple.setClickable(false);
            }

            holder.comments_ripple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_COMMENT, Integer.parseInt(user.getPost_id()), Integer.parseInt(user.getPost_id()), FlexibleTenpoActivity.this);
                    sHandler.sendMessageDelayed(msg, 750);
                }
            });

            holder.share_ripple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (Application_Gocci.transferUtility != null) {
                        new BottomSheet.Builder(context, R.style.BottomSheet_StyleDialog).sheet(R.menu.menu_share).listener(new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case R.id.facebook_share:
                                        Toast.makeText(FlexibleTenpoActivity.this, "シェアの準備をしています", Toast.LENGTH_LONG).show();
                                        Util.facebookVideoShare(context, shareDialog, user.getShare());
                                        break;
                                    case R.id.twitter_share:
                                        Util.twitterShare(context, holder.mVideoThumbnail, user.getRestname());
                                        break;
                                    case R.id.other_share:
                                        Toast.makeText(FlexibleTenpoActivity.this, "シェアの準備をしています", Toast.LENGTH_LONG).show();
                                        Util.instaVideoShare(context, user.getRestname(), user.getShare());
                                        break;
                                    case R.id.close:
                                        dialog.dismiss();
                                }
                            }
                        }).show();
                    } else {
                        Toast.makeText(FlexibleTenpoActivity.this, "もうちょっと待ってから押してみましょう", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            mViewHolderHash.put(holder, user.getPost_id());
        }

        @Override
        public int getItemCount() {
            return mTenpousers.size() + 1;
        }
    }

    class callClickHandler implements Runnable {
        public void run() {
            Intent intent = new Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:" + headerTenpoData.getTell()));
            startActivity(intent);
            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
        }
    }

    class gohereClickHandler implements Runnable {
        public void run() {
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + headerTenpoData.getLat() + "," + headerTenpoData.getLon() + "&mode=w");
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");
            startActivity(mapIntent);
            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
        }
    }
}