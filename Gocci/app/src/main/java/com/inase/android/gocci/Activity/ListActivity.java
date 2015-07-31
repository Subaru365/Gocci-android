package com.inase.android.gocci.Activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.andexert.library.RippleView;
import com.inase.android.gocci.Base.RoundedTransformation;
import com.inase.android.gocci.Event.NotificationNumberEvent;
import com.inase.android.gocci.R;
import com.inase.android.gocci.View.DrawerProfHeader;
import com.inase.android.gocci.common.Const;
import com.inase.android.gocci.common.SavedData;
import com.inase.android.gocci.common.Util;
import com.inase.android.gocci.data.HeaderData;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.squareup.otto.Subscribe;
import com.squareup.picasso.Picasso;

import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ListActivity extends AppCompatActivity {

    private int mCategory;
    private int mId;
    private String mUrl;
    private int isMypage; // 0　マイページでない　１　マイページ

    private ArrayList<HeaderData> users = new ArrayList<>();

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private SwipeRefreshLayout refresh;

    private FollowFollowerAdapter followefollowerAdapter;
    private UserCheerAdapter usercheerAdapter;
    private WantAdapter wantAdapter;
    private RestCheerAdapter restcheerAdapter;

    private Drawer result;

    public static void startListActivity(int id, int isMypage, int category, Activity startingActivity) {
        Intent intent = new Intent(startingActivity, ListActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("check", isMypage);
        intent.putExtra("category", category);
        startingActivity.startActivity(intent);
        startingActivity.overridePendingTransition(R.anim.activity_in, R.anim.activity_out);
    }

    private static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ListActivity activity
                    = (ListActivity) msg.obj;
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
                case Const.INTENT_TO_RESTPAGE:
                    FlexibleTenpoActivity.startTenpoActivity(msg.arg1, activity);
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
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follower_followee_cheer_list);

        Intent intent = getIntent();
        mCategory = intent.getIntExtra("category", 0);
        mId = intent.getIntExtra("id", 0);
        isMypage = intent.getIntExtra("check", 0);

        mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);
        refresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh.setRefreshing(true);
                if (Util.getConnectedState(ListActivity.this) != Util.NetworkStatus.OFF) {
                    getRefreshJSON(mUrl, mCategory, ListActivity.this);
                } else {
                    Toast.makeText(ListActivity.this, "通信に失敗しました", Toast.LENGTH_LONG).show();
                    refresh.setRefreshing(false);
                }
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
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
                                        sHandler.obtainMessage(Const.INTENT_TO_TIMELINE, 0, 0, ListActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 2) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_MYPAGE, 0, 0, ListActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 3) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_ADVICE, 0, 0, ListActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 4) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_POLICY, 0, 0, ListActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 5) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_LICENSE, 0, 0, ListActivity.this);
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

        switch (mCategory) {
            case Const.CATEGORY_FOLLOW:
                getSupportActionBar().setTitle("フォロー");
                followefollowerAdapter = new FollowFollowerAdapter(this);
                mUrl = Const.getFollowAPI(mId);
                break;
            case Const.CATEGORY_FOLLOWER:
                getSupportActionBar().setTitle("フォロワー");
                followefollowerAdapter = new FollowFollowerAdapter(this);
                mUrl = Const.getFollowerAPI(mId);
                break;
            case Const.CATEGORY_USER_CHEER:
                getSupportActionBar().setTitle("応援店");
                usercheerAdapter = new UserCheerAdapter(this);
                mUrl = Const.getUserCheerAPI(mId);
                break;
            case Const.CATEGORY_WANT:
                getSupportActionBar().setTitle("行きたい店");
                wantAdapter = new WantAdapter(this);
                mUrl = Const.getWantAPI(mId);
                break;
            case Const.CATEGORY_REST_CHEER:
                getSupportActionBar().setTitle("応援しているユーザー");
                restcheerAdapter = new RestCheerAdapter(this);
                mUrl = Const.getRestCheerAPI(mId);
                break;
        }

        Log.e("ログ", mUrl);
        getJSON(mUrl, mCategory, this);
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

    @Subscribe
    public void subscribe(NotificationNumberEvent event) {
        Snackbar.make(mRecyclerView, event.mMessage, Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void getJSON(String url, int category, Context context) {
        switch (category) {
            case Const.CATEGORY_FOLLOW:
                getFollowJSON(url, context);
                break;
            case Const.CATEGORY_FOLLOWER:
                getFollowerJSON(url, context);
                break;
            case Const.CATEGORY_USER_CHEER:
                getUserCheerJSON(url, context);
                break;
            case Const.CATEGORY_WANT:
                getWantJSON(url, context);
                break;
            case Const.CATEGORY_REST_CHEER:
                getRestCheerJSON(url, context);
                break;
        }
    }

    private void getRefreshJSON(String url, int category, Context context) {
        switch (category) {
            case Const.CATEGORY_FOLLOW:
                getRefreshFollowJSON(url, context);
                break;
            case Const.CATEGORY_FOLLOWER:
                getRefreshFollowerJSON(url, context);
                break;
            case Const.CATEGORY_USER_CHEER:
                getRefreshUserCheerJSON(url, context);
                break;
            case Const.CATEGORY_WANT:
                getRefreshWantJSON(url, context);
                break;
            case Const.CATEGORY_REST_CHEER:
                getRefreshRestCheerJSON(url, context);
                break;
        }
    }

    private void getFollowJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int user_id = jsonObject.getInt("user_id");
                        String username = jsonObject.getString("username");
                        String profile_img = jsonObject.getString("profile_img");
                        int follow_flag = jsonObject.getInt("follow_flag");

                        HeaderData user = new HeaderData();
                        user.setUser_id(user_id);
                        user.setUsername(username);
                        user.setProfile_img(profile_img);
                        user.setFollow_flag(follow_flag);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mRecyclerView.setAdapter(followefollowerAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

        });

    }

    private void getFollowerJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int user_id = jsonObject.getInt("user_id");
                        String username = jsonObject.getString("username");
                        String profile_img = jsonObject.getString("profile_img");
                        int follow_flag = jsonObject.getInt("follow_flag");

                        HeaderData user = new HeaderData();
                        user.setUser_id(user_id);
                        user.setUsername(username);
                        user.setProfile_img(profile_img);
                        user.setFollow_flag(follow_flag);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mRecyclerView.setAdapter(followefollowerAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

        });

    }

    private void getUserCheerJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int rest_id = jsonObject.getInt("rest_id");
                        String restname = jsonObject.getString("restname");
                        String locality = jsonObject.getString("locality");

                        HeaderData user = new HeaderData();
                        user.setRest_id(rest_id);
                        user.setRestname(restname);
                        user.setLocality(locality);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mRecyclerView.setAdapter(usercheerAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

        });

    }

    private void getWantJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int rest_id = jsonObject.getInt("rest_id");
                        String restname = jsonObject.getString("restname");
                        String locality = jsonObject.getString("locality");

                        HeaderData user = new HeaderData();
                        user.setRest_id(rest_id);
                        user.setRestname(restname);
                        user.setLocality(locality);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mRecyclerView.setAdapter(wantAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

        });

    }

    private void getRestCheerJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int user_id = jsonObject.getInt("user_id");
                        String username = jsonObject.getString("username");
                        String profile_img = jsonObject.getString("profile_img");
                        int follow_flag = jsonObject.getInt("follow_flag");

                        HeaderData user = new HeaderData();
                        user.setUser_id(user_id);
                        user.setUsername(username);
                        user.setProfile_img(profile_img);
                        user.setFollow_flag(follow_flag);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mRecyclerView.setAdapter(restcheerAdapter);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

        });

    }

    private void getRefreshFollowJSON(String url,  Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                users.clear();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int user_id = jsonObject.getInt("user_id");
                        String username = jsonObject.getString("username");
                        String profile_img = jsonObject.getString("profile_img");
                        int follow_flag = jsonObject.getInt("follow_flag");

                        HeaderData user = new HeaderData();
                        user.setUser_id(user_id);
                        user.setUsername(username);
                        user.setProfile_img(profile_img);
                        user.setFollow_flag(follow_flag);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                followefollowerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

            @Override
            public void onFinish() {
                refresh.setRefreshing(false);
            }

        });

    }

    private void getRefreshFollowerJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                users.clear();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int user_id = jsonObject.getInt("user_id");
                        String username = jsonObject.getString("username");
                        String profile_img = jsonObject.getString("profile_img");
                        int follow_flag = jsonObject.getInt("follow_flag");

                        HeaderData user = new HeaderData();
                        user.setUser_id(user_id);
                        user.setUsername(username);
                        user.setProfile_img(profile_img);
                        user.setFollow_flag(follow_flag);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                followefollowerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

            @Override
            public void onFinish() {
                refresh.setRefreshing(false);
            }
        });

    }

    private void getRefreshUserCheerJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                users.clear();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int rest_id = jsonObject.getInt("rest_id");
                        String restname = jsonObject.getString("restname");
                        String locality = jsonObject.getString("locality");

                        HeaderData user = new HeaderData();
                        user.setRest_id(rest_id);
                        user.setRestname(restname);
                        user.setLocality(locality);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                usercheerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

            @Override
            public void onFinish() {
                refresh.setRefreshing(false);
            }

        });

    }

    private void getRefreshWantJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                users.clear();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int rest_id = jsonObject.getInt("rest_id");
                        String restname = jsonObject.getString("restname");
                        String locality = jsonObject.getString("locality");

                        HeaderData user = new HeaderData();
                        user.setRest_id(rest_id);
                        user.setRestname(restname);
                        user.setLocality(locality);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                wantAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

            @Override
            public void onFinish() {
                refresh.setRefreshing(false);
            }

        });

    }

    private void getRefreshRestCheerJSON(String url, Context context) {
        Const.asyncHttpClient.setCookieStore(SavedData.getCookieStore(context));
        Const.asyncHttpClient.get(this, url, new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                Log.e("ジェイソン成功", String.valueOf(response));
                users.clear();
                try {
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject jsonObject = response.getJSONObject(i);
                        int user_id = jsonObject.getInt("user_id");
                        String username = jsonObject.getString("username");
                        String profile_img = jsonObject.getString("profile_img");
                        int follow_flag = jsonObject.getInt("follow_flag");

                        HeaderData user = new HeaderData();
                        user.setUser_id(user_id);
                        user.setUsername(username);
                        user.setProfile_img(profile_img);
                        user.setFollow_flag(follow_flag);

                        users.add(user);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                restcheerAdapter.notifyDataSetChanged();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                Log.e("ジェイソン失敗", String.valueOf(errorResponse));
            }

        });

    }

    public static class FollowFollowerViewHolder extends RecyclerView.ViewHolder {
        ImageView userpicture;
        TextView username;
        ImageView addfollowButton;
        ImageView deletefollowButton;
        RippleView accountRipple;

        public FollowFollowerViewHolder(View view) {
            super(view);
            this.userpicture = (ImageView) view.findViewById(R.id.follower_followee_picture);
            this.username = (TextView) view.findViewById(R.id.username);
            this.addfollowButton = (ImageView) view.findViewById(R.id.addfollowButton);
            this.deletefollowButton = (ImageView) view.findViewById(R.id.deletefollowButton);
            this.accountRipple = (RippleView) view.findViewById(R.id.accountButton);
        }
    }

    public static class UserCheerViewHolder extends RecyclerView.ViewHolder {
        ImageView restpicture;
        TextView restname;
        TextView locality;
        //ImageView deletecheerButton;
        //RippleView cheerRipple;

        public UserCheerViewHolder(View view) {
            super(view);
            this.restpicture = (ImageView) view.findViewById(R.id.cheer_picture);
            this.restname = (TextView) view.findViewById(R.id.restname);
            this.locality = (TextView) view.findViewById(R.id.locality);
            //this.deletecheerButton = (ImageView) view.findViewById(R.id.deleteCheerButton);
            //this.cheerRipple = (RippleView) view.findViewById(R.id.cheerButton);
        }
    }

    public static class WantViewHolder extends RecyclerView.ViewHolder {
        ImageView restpicture;
        TextView restname;
        TextView locality;
        ImageView deletewantButton;
        ImageView addwantButton;
        RippleView wantRipple;

        public WantViewHolder(View view) {
            super(view);
            this.restpicture = (ImageView) view.findViewById(R.id.want_picture);
            this.restname = (TextView) view.findViewById(R.id.restname);
            this.locality = (TextView) view.findViewById(R.id.locality);
            this.deletewantButton = (ImageView) view.findViewById(R.id.deletewantButton);
            this.addwantButton = (ImageView) view.findViewById(R.id.addwantButton);
            this.wantRipple = (RippleView) view.findViewById(R.id.wantButton);
        }
    }

    public static class RestCheerViewHolder extends RecyclerView.ViewHolder {
        ImageView userpicture;
        TextView username;
        ImageView addfollowButton;
        ImageView deletefollowButton;
        RippleView accountRipple;

        public RestCheerViewHolder(View view) {
            super(view);
            this.userpicture = (ImageView) view.findViewById(R.id.tenpo_cheer_picture);
            this.username = (TextView) view.findViewById(R.id.username);
            this.addfollowButton = (ImageView) view.findViewById(R.id.addfollowButton);
            this.deletefollowButton = (ImageView) view.findViewById(R.id.deletefollowButton);
            this.accountRipple = (RippleView) view.findViewById(R.id.accountButton);
        }
    }

    public class FollowFollowerAdapter extends RecyclerView.Adapter<FollowFollowerViewHolder> {

        private Context mContext;

        public FollowFollowerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public FollowFollowerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext)
                    .inflate(R.layout.cell_follow_follower, parent, false);
            return new FollowFollowerViewHolder(v);
        }

        @Override
        public void onBindViewHolder(FollowFollowerViewHolder viewHolder, final int position) {
            final HeaderData user = users.get(position);

            viewHolder.username.setText(user.getUsername());

            Picasso.with(mContext)
                    .load(user.getProfile_img())
                    .placeholder(R.drawable.ic_userpicture)
                    .transform(new RoundedTransformation())
                    .into(viewHolder.userpicture);

            viewHolder.username.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_USERPAGE, user.getUser_id(), user.getUser_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });

            viewHolder.userpicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_USERPAGE, user.getUser_id(), user.getUser_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });


            switch (mCategory) {
                case Const.CATEGORY_FOLLOW:
                    if (isMypage == 1) {
                        viewHolder.deletefollowButton.setVisibility(View.VISIBLE);
                        final FollowFollowerViewHolder finalViewHolder = viewHolder;
                        viewHolder.accountRipple.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (finalViewHolder.deletefollowButton.isShown()) {
                                    finalViewHolder.deletefollowButton.setVisibility(View.INVISIBLE);
                                    finalViewHolder.addfollowButton.setVisibility(View.VISIBLE);
                                    Util.unfollowAsync(ListActivity.this, user);
                                } else {
                                    finalViewHolder.deletefollowButton.setVisibility(View.VISIBLE);
                                    finalViewHolder.addfollowButton.setVisibility(View.INVISIBLE);
                                    Util.followAsync(ListActivity.this, user);
                                }
                            }
                        });
                    }
                    break;
                case Const.CATEGORY_FOLLOWER:
                    if (isMypage == 1) {
                        if (user.getFollow_flag() == 0) {
                            viewHolder.addfollowButton.setVisibility(View.VISIBLE);
                        } else {
                            viewHolder.deletefollowButton.setVisibility(View.VISIBLE);
                        }
                        final FollowFollowerViewHolder finalViewHolder1 = viewHolder;
                        viewHolder.accountRipple.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (finalViewHolder1.addfollowButton.isShown()) {
                                    finalViewHolder1.addfollowButton.setVisibility(View.INVISIBLE);
                                    finalViewHolder1.deletefollowButton.setVisibility(View.VISIBLE);
                                    Util.followAsync(ListActivity.this, user);
                                } else {
                                    finalViewHolder1.addfollowButton.setVisibility(View.VISIBLE);
                                    finalViewHolder1.deletefollowButton.setVisibility(View.INVISIBLE);
                                    Util.unfollowAsync(ListActivity.this, user);
                                }
                            }
                        });
                    }
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

    public class UserCheerAdapter extends RecyclerView.Adapter<UserCheerViewHolder> {

        private Context mContext;

        public UserCheerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public UserCheerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext)
                    .inflate(R.layout.cell_cheer, parent, false);
            return new UserCheerViewHolder(v);
        }

        @Override
        public void onBindViewHolder(UserCheerViewHolder viewHolder, final int position) {
            final HeaderData user = users.get(position);

            viewHolder.restname.setText(user.getRestname());
            viewHolder.locality.setText(user.getLocality());

            /*
            Picasso.with(mContext)
                    .load(user.getProfile_img())
                    .placeholder(R.drawable.ic_userpicture)
                    .transform(new RoundedTransformation())
                    .into(viewHolder.restpicture);
                    */

            viewHolder.restname.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_RESTPAGE, user.getRest_id(), user.getRest_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });

            viewHolder.restpicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_RESTPAGE, user.getRest_id(), user.getRest_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

    public class WantAdapter extends RecyclerView.Adapter<WantViewHolder> {
        private Context mContext;

        public WantAdapter(Context context) {
            mContext = context;
        }

        @Override
        public WantViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext)
                    .inflate(R.layout.cell_want, parent, false);
            return new WantViewHolder(v);
        }

        @Override
        public void onBindViewHolder(WantViewHolder viewHolder, final int position) {
            final HeaderData user = users.get(position);

            viewHolder.restname.setText(user.getRestname());
            viewHolder.locality.setText(user.getLocality());

            /*
            Picasso.with(mContext)
                    .load(user.getProfile_img())
                    .placeholder(R.drawable.ic_userpicture)
                    .transform(new RoundedTransformation())
                    .into(viewHolder.restpicture);
                    */

            viewHolder.restname.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_RESTPAGE, user.getRest_id(), user.getRest_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });

            viewHolder.restpicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_RESTPAGE, user.getRest_id(), user.getRest_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });

            viewHolder.deletewantButton.setVisibility(View.VISIBLE);
            final WantViewHolder finalViewHolder = viewHolder;
            viewHolder.wantRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (finalViewHolder.deletewantButton.isShown()) {
                        finalViewHolder.deletewantButton.setVisibility(View.INVISIBLE);
                        finalViewHolder.addwantButton.setVisibility(View.VISIBLE);
                        Util.unwantAsync(ListActivity.this, user);
                    } else {
                        finalViewHolder.deletewantButton.setVisibility(View.VISIBLE);
                        finalViewHolder.addwantButton.setVisibility(View.INVISIBLE);
                        Util.wantAsync(ListActivity.this, user);
                    }
                }
            });

        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }

    public class RestCheerAdapter extends RecyclerView.Adapter<RestCheerViewHolder> {
        private Context mContext;

        public RestCheerAdapter(Context context) {
            mContext = context;
        }

        @Override
        public RestCheerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext)
                    .inflate(R.layout.cell_tenpo_cheer, parent, false);
            return new RestCheerViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RestCheerViewHolder viewHolder, final int position) {
            final HeaderData user = users.get(position);

            viewHolder.username.setText(user.getUsername());

            Picasso.with(mContext)
                    .load(user.getProfile_img())
                    .placeholder(R.drawable.ic_userpicture)
                    .transform(new RoundedTransformation())
                    .into(viewHolder.userpicture);

            viewHolder.username.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_USERPAGE, user.getUser_id(), user.getUser_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });

            viewHolder.userpicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Message msg =
                            sHandler.obtainMessage(Const.INTENT_TO_USERPAGE, user.getUser_id(), user.getUser_id(), ListActivity.this);
                    sHandler.sendMessageDelayed(msg, 50);
                }
            });

            if (user.getFollow_flag() == 0) {
                viewHolder.addfollowButton.setVisibility(View.VISIBLE);
            } else {
                viewHolder.deletefollowButton.setVisibility(View.VISIBLE);
            }
            final RestCheerViewHolder finalViewHolder1 = viewHolder;
            viewHolder.accountRipple.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (finalViewHolder1.addfollowButton.isShown()) {
                        finalViewHolder1.addfollowButton.setVisibility(View.INVISIBLE);
                        finalViewHolder1.deletefollowButton.setVisibility(View.VISIBLE);
                        Util.followAsync(ListActivity.this, user);
                    } else {
                        finalViewHolder1.addfollowButton.setVisibility(View.VISIBLE);
                        finalViewHolder1.deletefollowButton.setVisibility(View.INVISIBLE);
                        Util.unfollowAsync(ListActivity.this, user);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }
    }
}