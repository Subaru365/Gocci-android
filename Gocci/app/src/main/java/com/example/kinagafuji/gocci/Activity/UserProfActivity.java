package com.example.kinagafuji.gocci.Activity;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.kinagafuji.gocci.Base.BaseActivity;
import com.example.kinagafuji.gocci.Base.CustomProgressDialog;
import com.example.kinagafuji.gocci.R;
import com.example.kinagafuji.gocci.View.CommentView;
import com.example.kinagafuji.gocci.data.RoundedTransformation;
import com.example.kinagafuji.gocci.data.UserData;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

import me.drakeet.materialdialog.MaterialDialog;

public class UserProfActivity extends BaseActivity implements ListView.OnScrollListener {

    private static final String TAG_POST_ID = "post_id";
    private static final String TAG_USER_ID = "user_id";
    private static final String TAG_USER_NAME = "user_name";
    private static final String TAG_PICTURE = "picture";
    private static final String TAG_MOVIE = "movie";
    private static final String TAG_RESTNAME = "restname";
    private static final String TAG_GOODNUM = "goodnum";
    private static final String TAG_COMMENT_NUM = "comment_num";
    private static final String TAG_THUMBNAIL = "thumbnail";
    private static final String TAG_STAR_EVALUATION = "star_evaluation";
    private static final String sSignupUrl = "http://api-gocci.jp/login/";
    private static final String sGoodUrl = "http://api-gocci.jp/goodinsert/";
    private static final String sDataurl = "http://api-gocci.jp/login/";
    public int mGoodCommePosition;
    public String mNextGoodnum;
    public String currentgoodnum;
    public String mNextCommentnum;
    private String mUser_name;
    private String mName;
    private String mPictureImageUrl;
    private ListView mUserProfListView;
    private UserProfAdapter mUserProfAdapter;
    private String mProfUrl;
    private SwipeRefreshLayout mUserProfSwipe;
    private ArrayList<UserData> mUserProfusers = new ArrayList<UserData>();
    private String mEncodeUser_name;
    private int mShowPosition;
    private VideoView nextVideo;
    private NameHolder nameHolder;
    private RestHolder restHolder;
    private VideoHolder videoHolder;
    private CommentHolder commentHolder;
    private LikeCommentHolder likeCommentHolder;
    private boolean mBusy = false;
    private CustomProgressDialog mUserProfDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_prof);

        Intent userintent = getIntent();
        mUser_name = userintent.getStringExtra("username");
        mName = userintent.getStringExtra("name");
        mPictureImageUrl = userintent.getStringExtra("pictureImageUrl");

        try {
            mEncodeUser_name = URLEncoder.encode(mUser_name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        mProfUrl = "http://api-gocci.jp/mypage/?user_name=" + mEncodeUser_name;

        new UserProfAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mProfUrl);
        mUserProfDialog = new CustomProgressDialog(this);
        mUserProfDialog.setCancelable(false);
        mUserProfDialog.show();

        mUserProfListView = (ListView) findViewById(R.id.userProfListView);

        mUserProfAdapter = new UserProfAdapter(this, 0, mUserProfusers);

        mUserProfListView.setDivider(null);
        // スクロールバーを表示しない
        mUserProfListView.setVerticalScrollBarEnabled(false);
        // カード部分をselectorにするので、リストのselectorは透明にする
        mUserProfListView.setSelector(android.R.color.transparent);

        mUserProfListView.setAdapter(mUserProfAdapter);

        mUserProfListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserData country = mUserProfusers.get(position);
                int line = (position / 5) * 5;
                int pos = position - line;

                switch (pos) {
                    case 0:
                        //名前部分のview　プロフィール画面へ
                        break;
                    case 1:
                        //動画のview
                        //クリックしたら止まるくらい
                        break;
                    case 2:
                        //コメントのview
                        //とくになんもしない
                        break;
                    case 3:
                        //レストランのview
                        //レストラン画面に飛ぼうか
                        Intent intent = new Intent(UserProfActivity.this, TenpoActivity.class);
                        intent.putExtra("restname", country.getRest_name());
                        intent.putExtra("locality", country.getLocality());
                        startActivity(intent);
                        break;
                    case 4:
                        //いいね　コメント　シェア
                        break;
                }
            }
        });

        mUserProfSwipe = (SwipeRefreshLayout) findViewById(R.id.swipe_user_prof);
        mUserProfSwipe.setColorSchemeColors(R.color.main_color_light, R.color.gocci, R.color.main_color_dark, R.color.window_bg);
        mUserProfSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
//Handle the refresh then call
                new UserProfAsyncTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mProfUrl);
                mUserProfDialog = new CustomProgressDialog(UserProfActivity.this);
                mUserProfDialog.setCancelable(false);
                mUserProfDialog.show();
                mUserProfSwipe.setRefreshing(false);

            }
        });

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        switch (scrollState) {

            // スクロールしていない
            case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:

                mBusy = false;

                break;

            // スクロール中
            case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
                mBusy = true;
                break;

            // はじいたとき
            case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
                mBusy = true;
                break;
        }

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    private static class NameHolder {
        ImageView circleImage;
        TextView user_name;
    }

    private static class VideoHolder {
        VideoView movie;
        ImageView mVideoThumbnail;
    }

    private static class CommentHolder {
        RatingBar star_evaluation;
        TextView likesnumber;
        TextView commentsnumber;
        TextView sharenumber;
    }

    private static class RestHolder {
        ImageView restaurantImage;
        TextView locality;
        TextView rest_name;
    }

    private static class LikeCommentHolder {
        ImageView likes;
        ImageView comments;
        ImageView share;
    }

    public class UserProfAsyncTask extends AsyncTask<String, String, Integer> {

        @Override
        protected Integer doInBackground(String... params) {
            String url = params[0];

            HttpClient client = new DefaultHttpClient();

            HttpPost method = new HttpPost(sSignupUrl);

            ArrayList<NameValuePair> contents = new ArrayList<NameValuePair>();
            contents.add(new BasicNameValuePair("user_name", mName));
            contents.add(new BasicNameValuePair("picture", mPictureImageUrl));
            Log.d("読み取り", mName + "と" + mPictureImageUrl);

            String body = null;
            try {
                method.setEntity(new UrlEncodedFormEntity(contents, "utf-8"));
                HttpResponse res = client.execute(method);
                Log.d("TAGだよ", "反応");
                HttpEntity entity = res.getEntity();
                body = EntityUtils.toString(entity, "UTF-8");
                Log.d("bodyの中身だよ", body);
            } catch (Exception e) {
                e.printStackTrace();
            }

            HttpGet request = new HttpGet(url);
            HttpResponse httpResponse = null;

            try {
                httpResponse = client.execute(request);
            } catch (Exception e) {
                Log.d("error", String.valueOf(e));
            }

            int status = httpResponse.getStatusLine().getStatusCode();

            if (HttpStatus.SC_OK == status) {
                String mProfData = null;
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    httpResponse.getEntity().writeTo(outputStream);
                    mProfData = outputStream.toString(); // JSONデータ
                    Log.d("data", mProfData);
                } catch (Exception e) {
                    Log.d("error", String.valueOf(e));
                }

                mUserProfusers.clear();

                try {
                    JSONArray jsonArray = new JSONArray(mProfData);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        String post_id = jsonObject.getString(TAG_POST_ID);
                        Integer user_id = jsonObject.getInt(TAG_USER_ID);
                        String user_name = jsonObject.getString(TAG_USER_NAME);
                        String picture = jsonObject.getString(TAG_PICTURE);
                        String movie = jsonObject.getString(TAG_MOVIE);
                        String restname = jsonObject.getString(TAG_RESTNAME);
                        Integer goodnum = jsonObject.getInt(TAG_GOODNUM);
                        Integer comment_num = jsonObject.getInt(TAG_COMMENT_NUM);
                        String thumbnail = jsonObject.getString(TAG_THUMBNAIL);
                        Integer star_evaluation = jsonObject.getInt(TAG_STAR_EVALUATION);
                        //String locality = jsonObject.getString(TAG_LOCALITY);


                        UserData user1 = new UserData();
                        user1.setUser_name(user_name);
                        user1.setPicture(picture);
                        mUserProfusers.add(user1);

                        UserData user2 = new UserData();
                        user2.setMovie(movie);
                        user2.setThumbnail(thumbnail);
                        mUserProfusers.add(user2);

                        UserData user3 = new UserData();
                        user3.setComment_num(comment_num);
                        user3.setgoodnum(goodnum);
                        user3.setStar_evaluation(star_evaluation);
                        mUserProfusers.add(user3);

                        UserData user4 = new UserData();
                        user4.setRest_name(restname);
                        //user4.setLocality(locality);
                        mUserProfusers.add(user4);

                        UserData user5 = new UserData();
                        user5.setPost_id(post_id);
                        user5.setUser_id(user_id);
                        mUserProfusers.add(user5);

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.d("error", String.valueOf(e));
                }
            } else {
                Log.d("JSONSampleActivity", "Status" + status);
            }

            return status;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null && result == HttpStatus.SC_OK) {
                //ListViewの最読み込み
                mUserProfListView.invalidateViews();
                mUserProfAdapter.notifyDataSetChanged();
            } else {
                //通信失敗した際のエラー処理
                Toast.makeText(UserProfActivity.this, "タイムラインの取得に失敗しました。", Toast.LENGTH_SHORT).show();
            }
            mUserProfDialog.dismiss();
        }
    }

    public class UserProfAdapter extends ArrayAdapter<UserData> {

        public UserProfAdapter(Context context, int viewResourceId, ArrayList<UserData> userprofusers) {
            super(context, viewResourceId, userprofusers);
        }

        @Override
        public int getItemViewType(int position) {
            int line = (position / 5) * 5;
            int pos = position - line;
            Log.e("どんなposition/どのタイミングで帰ってくるのか？", String.valueOf(position));

            switch (pos) {
                case 0:
                    return 0;
                case 1:
                    return 1;
                case 2:
                    return 2;
                case 3:
                    return 3;
                default:
                    return 4;
            }
        }

        @Override
        public int getViewTypeCount() {
            return 5;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            final UserData user = this.getItem(position);

            switch (getItemViewType(position)) {

                case 0:
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = inflater.inflate(R.layout.name_picture_bar, null);

                        nameHolder = new NameHolder();
                        nameHolder.circleImage = (ImageView) convertView.findViewById(R.id.circleImage);
                        nameHolder.user_name = (TextView) convertView.findViewById(R.id.user_name);

                        convertView.setTag(nameHolder);
                    } else {
                        nameHolder = (NameHolder) convertView.getTag();
                    }

                    nameHolder.user_name.setText(user.getUser_name());

                    Picasso.with(getContext())
                            .load(user.getPicture())
                            .resize(50, 50)
                            .placeholder(R.drawable.ic_userpicture)
                            .centerCrop()
                            .transform(new RoundedTransformation())
                            .into(nameHolder.circleImage);

                    break;

                case 1:
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = inflater.inflate(R.layout.video_bar, null);

                        videoHolder = new VideoHolder();
                        videoHolder.movie = (VideoView) convertView.findViewById(R.id.videoView);
                        videoHolder.mVideoThumbnail = (ImageView) convertView.findViewById(R.id.video_thumbnail);

                        convertView.setTag(videoHolder);
                    } else {
                        videoHolder = (VideoHolder) convertView.getTag();
                    }

                    Picasso.with(getContext())
                            .load(user.getThumbnail())
                            .placeholder(R.color.videobackground)
                            .into(videoHolder.mVideoThumbnail);
                    videoHolder.mVideoThumbnail.setVisibility(View.VISIBLE);

                    if (!mBusy) {

                        videoHolder.movie.setVideoURI(Uri.parse(user.getMovie()));
                        Log.e("読み込みました", user.getMovie());
                        videoHolder.movie.requestFocus();
                        videoHolder.movie.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                nextVideo = (VideoView) mUserProfListView.findViewWithTag(mShowPosition);

                                if (nextVideo != null) {
                                    nextVideo.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                        @Override
                                        public void onPrepared(MediaPlayer mp) {
                                            mp.stop();
                                            Log.e("TAG", "pause : " + mShowPosition);
                                            //nextVideo.stopPlayback();
                                            //nextVideo.pause();
                                        }
                                    });
                                }

                                videoHolder.mVideoThumbnail.setVisibility(View.GONE);
                                videoHolder.movie.start();

                                mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    @Override
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.start();
                                        mp.setLooping(true);
                                    }
                                });

                                Log.e("TAG", "start : " + position);
                                mShowPosition = position;
                            }
                        });

                    }

                    break;

                case 2:
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = inflater.inflate(R.layout.comment_bar, null);

                        commentHolder = new CommentHolder();
                        commentHolder.star_evaluation = (RatingBar) convertView.findViewById(R.id.star_evaluation);
                        commentHolder.likesnumber = (TextView) convertView.findViewById(R.id.likesnumber);
                        commentHolder.commentsnumber = (TextView) convertView.findViewById(R.id.commentsnumber);
                        commentHolder.sharenumber = (TextView) convertView.findViewById(R.id.sharenumber);

                        convertView.setTag(commentHolder);
                    } else {
                        commentHolder = (CommentHolder) convertView.getTag();
                    }

                    commentHolder.likesnumber.setText(String.valueOf(user.getgoodnum()));
                    commentHolder.commentsnumber.setText(String.valueOf(user.getComment_num()));

                    commentHolder.star_evaluation.setIsIndicator(true);
                    commentHolder.star_evaluation.setRating((float) user.getStar_evaluation());

                    mNextGoodnum = String.valueOf(user.getgoodnum() + 1);
                    currentgoodnum = String.valueOf((user.getgoodnum()));
                    mNextCommentnum = String.valueOf((user.getComment_num() + 1));

                    break;

                case 3:
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = inflater.inflate(R.layout.restaurant_bar, null);

                        restHolder = new RestHolder();
                        restHolder.restaurantImage = (ImageView) convertView.findViewById(R.id.restaurantImage);
                        restHolder.rest_name = (TextView) convertView.findViewById(R.id.rest_name);
                        restHolder.locality = (TextView) convertView.findViewById(R.id.locality);

                        convertView.setTag(restHolder);
                    } else {
                        restHolder = (RestHolder) convertView.getTag();
                    }

                    restHolder.rest_name.setText(user.getRest_name());
                    restHolder.locality.setText(user.getLocality());

                    break;

                default:
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                        convertView = inflater.inflate(R.layout.likes_comments_bar, null);

                        likeCommentHolder = new LikeCommentHolder();
                        likeCommentHolder.likes = (ImageView) convertView.findViewById(R.id.likes);
                        likeCommentHolder.comments = (ImageView) convertView.findViewById(R.id.comments);
                        likeCommentHolder.share = (ImageView) convertView.findViewById(R.id.share);

                        convertView.setTag(likeCommentHolder);
                    } else {
                        likeCommentHolder = (LikeCommentHolder) convertView.getTag();
                    }

                    //クリックされた時の処理
                    if (mGoodCommePosition == position) {
                        likeCommentHolder.likes.setClickable(false);
                        likeCommentHolder.likes.setBackgroundResource(R.drawable.ic_like_orange);
                    }

                    likeCommentHolder.likes.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.e("いいねをクリック", user.getPost_id() + mNextGoodnum);
                            mGoodCommePosition = position;

                            likeCommentHolder.likes.setClickable(false);
                            commentHolder.likesnumber.setText(mNextGoodnum);
                            //画像差し込み
                            likeCommentHolder.likes.setBackgroundResource(R.drawable.ic_like_orange);

                            new UserProfGoodnumTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, user.getPost_id());
                        }
                    });

                    likeCommentHolder.comments.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.e("コメントをクリック", "コメント！" + user.getPost_id());
                            commentHolder.commentsnumber.setText(mNextCommentnum);

                            //引数に入れたい値を入れていく
                            View commentView = new CommentView(UserProfActivity.this, mName, mPictureImageUrl, user.getPost_id());

                            MaterialDialog mMaterialDialog = new MaterialDialog(UserProfActivity.this)
                                    .setContentView(commentView)
                                    .setCanceledOnTouchOutside(true);
                            mMaterialDialog.show();
                        }
                    });

                    break;
            }

            return convertView;

        }
    }

    public class UserProfGoodnumTask extends AsyncTask<String, String, Integer> {
        private int mStatus;
        private int mStatus2;
        private int mStatus3;

        @Override
        protected Integer doInBackground(String... params) {
            String param = params[0];

            HttpClient client = new DefaultHttpClient();

            HttpPost method = new HttpPost(sDataurl);

            ArrayList<NameValuePair> contents = new ArrayList<NameValuePair>();
            contents.add(new BasicNameValuePair("user_name", mName));
            contents.add(new BasicNameValuePair("picture", mPictureImageUrl));
            Log.d("読み取り", mName + "と" + mPictureImageUrl);

            String body = null;
            try {
                method.setEntity(new UrlEncodedFormEntity(contents, "utf-8"));
                HttpResponse res = client.execute(method);
                mStatus = res.getStatusLine().getStatusCode();
                Log.d("TAGだよ", "反応");
                HttpEntity entity = res.getEntity();
                body = EntityUtils.toString(entity, "UTF-8");
                Log.d("bodyの中身だよ", body);
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (HttpStatus.SC_OK == mStatus) {

                HttpPost goodnummethod = new HttpPost(sGoodUrl);

                ArrayList<NameValuePair> goodnumcontents = new ArrayList<NameValuePair>();
                goodnumcontents.add(new BasicNameValuePair("post_id", param));
                Log.d("読み取り", param);

                String goodnumbody = null;
                try {
                    goodnummethod.setEntity(new UrlEncodedFormEntity(goodnumcontents, "utf-8"));
                    HttpResponse goodnumres = client.execute(goodnummethod);
                    mStatus2 = goodnumres.getStatusLine().getStatusCode();
                    Log.d("TAGだよ", "反応");
                    HttpEntity goodnumentity = goodnumres.getEntity();
                    goodnumbody = EntityUtils.toString(goodnumentity, "UTF-8");
                    Log.d("bodyの中身だよ", goodnumbody);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (HttpStatus.SC_OK == mStatus2) {

                HttpGet request = new HttpGet(mProfUrl);
                HttpResponse httpResponse = null;

                try {
                    httpResponse = client.execute(request);
                } catch (Exception e) {
                    Log.d("error", String.valueOf(e));
                }

                mStatus3 = httpResponse.getStatusLine().getStatusCode();

                if (HttpStatus.SC_OK == mStatus3) {
                    String mProfData = null;
                    try {
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        httpResponse.getEntity().writeTo(outputStream);
                        mProfData = outputStream.toString(); // JSONデータ
                        Log.d("data", mProfData);
                    } catch (Exception e) {
                        Log.d("error", String.valueOf(e));
                    }

                    mUserProfusers.clear();

                    try {
                        JSONArray jsonArray = new JSONArray(mProfData);

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);

                            String post_id = jsonObject.getString(TAG_POST_ID);
                            Integer user_id = jsonObject.getInt(TAG_USER_ID);
                            String user_name = jsonObject.getString(TAG_USER_NAME);
                            String picture = jsonObject.getString(TAG_PICTURE);
                            String movie = jsonObject.getString(TAG_MOVIE);
                            String restname = jsonObject.getString(TAG_RESTNAME);
                            Integer goodnum = jsonObject.getInt(TAG_GOODNUM);
                            Integer comment_num = jsonObject.getInt(TAG_COMMENT_NUM);
                            String thumbnail = jsonObject.getString(TAG_THUMBNAIL);
                            Integer star_evaluation = jsonObject.getInt(TAG_STAR_EVALUATION);
                            //String locality = jsonObject.getString(TAG_LOCALITY);


                            UserData user1 = new UserData();
                            user1.setUser_name(user_name);
                            user1.setPicture(picture);
                            mUserProfusers.add(user1);

                            UserData user2 = new UserData();
                            user2.setMovie(movie);
                            user2.setThumbnail(thumbnail);
                            mUserProfusers.add(user2);

                            UserData user3 = new UserData();
                            user3.setComment_num(comment_num);
                            user3.setgoodnum(goodnum);
                            user3.setStar_evaluation(star_evaluation);
                            mUserProfusers.add(user3);

                            UserData user4 = new UserData();
                            user4.setRest_name(restname);
                            //user4.setLocality(locality);
                            mUserProfusers.add(user4);

                            UserData user5 = new UserData();
                            user5.setPost_id(post_id);
                            user5.setUser_id(user_id);
                            mUserProfusers.add(user5);

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("error", String.valueOf(e));
                    }
                } else {
                    Log.d("JSONSampleActivity", "Status" + mStatus3);
                }
            }

            return mStatus3;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result != null && result == HttpStatus.SC_OK) {
                //いいねが送れた処理　項目itemの更新
                View targetView = mUserProfListView.getChildAt((mGoodCommePosition - 2));
                mUserProfListView.getAdapter().getView((mGoodCommePosition - 2), targetView, mUserProfListView);
                Log.e("いいね追加成功", "成功しました");
            } else {
                //失敗のため、いいね取り消し
                commentHolder.likesnumber.setText(currentgoodnum);
                likeCommentHolder.likes.setClickable(true);
                likeCommentHolder.likes.setBackgroundResource(R.drawable.ic_like);
                Toast.makeText(UserProfActivity.this, "いいね追加に失敗しました。", Toast.LENGTH_SHORT).show();
            }

        }

    }

}