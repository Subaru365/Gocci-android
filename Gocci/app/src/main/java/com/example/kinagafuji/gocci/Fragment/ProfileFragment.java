package com.example.kinagafuji.gocci.Fragment;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.kinagafuji.gocci.Activity.TenpoActivity;
import com.example.kinagafuji.gocci.Base.BaseFragment;
import com.example.kinagafuji.gocci.Base.CustomProgressDialog;
import com.example.kinagafuji.gocci.R;
import com.example.kinagafuji.gocci.data.RoundedTransformation;
import com.example.kinagafuji.gocci.data.ToukouPopup;
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

public class ProfileFragment extends BaseFragment implements ListView.OnScrollListener {

    private static final String sSignupUrl = "http://api-gocci.jp/api/public/login/";
    private CustomProgressDialog mProfDialog;
    private ListView mProfListView;
    private ArrayList<UserData> mProfusers = new ArrayList<UserData>();
    private ProfAdapter mProfAdapter;
    private SwipeRefreshLayout mProfSwipe;

    private String mEncode_user_name;

    private String mName;
    private String mPictureImageUrl;

    private VideoHolder videoHolder;

    private int mShowPosition;

    private boolean mBusy = false;

    private static final String KEY_IMAGE_URL = "image_url";

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
    private static final String TAG_TELL = "tell";
    private static final String TAG_RESTNAME1 = "restname";
    private static final String TAG_CATEGORY = "category";
    private static final String TAG_LAT = "lat";
    private static final String TAG_LON = "lon";
    private static final String TAG_LOCALITY = "locality";
    private static final String TAG_DISTANCE = "distance";

    private TextView mPost_name;
    private ImageView mPost_Imageurl;


    public ProfileFragment newIntent(String name, String imageUrl) {
        ProfileFragment fragment = new ProfileFragment();
        Bundle args = new Bundle();
        args.putString(TAG_USER_NAME, name);
        if (imageUrl != null) {
            args.putString(KEY_IMAGE_URL, imageUrl);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        // FragmentのViewを返却
        View view3 = getActivity().getLayoutInflater().inflate(R.layout.fragment_profile,
                container, false);

        /*mPost_name = (TextView) view3.findViewById(R.id.post_name);
        mPost_Imageurl = (ImageView) view3.findViewById(R.id.post_Imageurl);
        */

        mProfListView = (ListView) view3.findViewById(R.id.proflist);

        mProfAdapter = new ProfAdapter(getActivity(), 0, mProfusers);

        mProfListView.setDivider(null);
        // スクロールバーを表示しない
        mProfListView.setVerticalScrollBarEnabled(false);
        // カード部分をselectorにするので、リストのselectorは透明にする
        mProfListView.setSelector(android.R.color.transparent);

        mProfListView.setAdapter(mProfAdapter);

        mProfListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                UserData country = mProfusers.get(position);
                int line = (position / 5) * 5;
                int pos = position - line;

                switch(pos) {
                    case 0:
                        //名前部分のview　プロフィール画面へ
                        //Signupを読み込みそう後回し
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
                        Intent intent = new Intent(getActivity(), TenpoActivity.class);
                        intent.putExtra("restname",country.getRest_name());
                        intent.putExtra("locality",country.getLocality());
                        startActivity(intent);
                        break;
                    case 4:
                        //いいね　コメント　シェア
                        break;
                }
            }
        });

        mProfSwipe = (SwipeRefreshLayout) view3.findViewById(R.id.swipe_prof);
        mProfSwipe.setColorSchemeColors(R.color.main_color_light, R.color.gocci, R.color.main_color_dark, R.color.window_bg);
        mProfSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
//Handle the refresh then call
                new ProfTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                mProfDialog = new CustomProgressDialog(getActivity());
                mProfDialog.setCancelable(false);
                mProfDialog.show();
                mProfSwipe.setRefreshing(false);

            }
        });

        return view3;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // 引数を取得
        Bundle args = getArguments();
        mName = args.getString(TAG_USER_NAME);
        mPictureImageUrl = args.getString(KEY_IMAGE_URL);

        new ProfTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mProfDialog = new CustomProgressDialog(getActivity());
        mProfDialog.setCancelable(false);
        mProfDialog.show();


        /*mPost_name.setText(mName);

        Picasso.with(getActivity())
                .load(mPictureImageUrl)
                .resize(80, 80)
                .placeholder(R.drawable.ic_userpicture)
                .centerCrop()
                .transform(new RoundedTransformation())
                .into(mPost_Imageurl);
                */
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();


    }

    @Override
    public void onResume() {

        super.onResume();

    }

    @Override
    public void onPause() {
        super.onPause();

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

    public class ProfTask extends AsyncTask<String, String, Integer> {

        @Override
        protected Integer doInBackground(String... strings) {
            HttpClient client1 = new DefaultHttpClient();

            HttpPost method = new HttpPost(sSignupUrl);

            ArrayList<NameValuePair> contents = new ArrayList<NameValuePair>();
            contents.add(new BasicNameValuePair("user_name", mName));
            contents.add(new BasicNameValuePair("picture", mPictureImageUrl));
            Log.d("読み取り", mName + "と" + mPictureImageUrl);

            String body = null;
            try {
                method.setEntity(new UrlEncodedFormEntity(contents, "utf-8"));
                HttpResponse res = client1.execute(method);
                Log.d("TAGだよ", "反応");
                HttpEntity entity = res.getEntity();
                body = EntityUtils.toString(entity, "UTF-8");
                Log.d("bodyの中身だよ", body);
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                mEncode_user_name = URLEncoder.encode(mName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            String mProfUrl = "http://api-gocci.jp/api/public/mypage/?user_name=" + mEncode_user_name;
            HttpGet request = new HttpGet(mProfUrl);
            HttpResponse httpResponse = null;

            try {
                httpResponse = client1.execute(request);
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

                try {
                    JSONArray jsonArray = new JSONArray(mProfData);

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(i);

                        String post_id = jsonObject.getString(TAG_POST_ID);
                        String user_id = jsonObject.getString(TAG_USER_ID);
                        String user_name = jsonObject.getString(TAG_USER_NAME);
                        String picture = jsonObject.getString(TAG_PICTURE);
                        String movie = jsonObject.getString(TAG_MOVIE);
                        String restname = jsonObject.getString(TAG_RESTNAME);
                        String goodnum = jsonObject.getString(TAG_GOODNUM);
                        String comment_num = jsonObject.getString(TAG_COMMENT_NUM);
                        String thumbnail = jsonObject.getString(TAG_THUMBNAIL);
                        String star_evaluation = jsonObject.getString(TAG_STAR_EVALUATION);
                        //String locality = jsonObject.getString(TAG_LOCALITY);


                        UserData user1 = new UserData();
                        user1.setUser_name(user_name);
                        user1.setPicture(picture);
                        mProfusers.add(user1);

                        UserData user2 = new UserData();
                        user2.setMovie(movie);
                        user2.setThumbnail(thumbnail);
                        mProfusers.add(user2);

                        UserData user3 = new UserData();
                        user3.setComment_num(comment_num);
                        user3.setgoodnum(goodnum);
                        user3.setStar_evaluation(star_evaluation);
                        mProfusers.add(user3);

                        UserData user4 = new UserData();
                        user4.setRest_name(restname);
                        //user4.setLocality(locality);
                        mProfusers.add(user4);

                        UserData user5 = new UserData();
                        user5.setPost_id(post_id);
                        user5.setUser_id(user_id);
                        mProfusers.add(user5);

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
                mProfListView.invalidateViews();
                mProfAdapter.notifyDataSetChanged();
            } else {
                //通信失敗した際のエラー処理
                Toast.makeText(getActivity().getApplicationContext(), "タイムラインの取得に失敗しました。", Toast.LENGTH_SHORT).show();
            }
            mProfDialog.dismiss();
        }
    }

    private static class NameHolder {
        ImageView circleImage;
        TextView user_name;
        TextView time;

        public NameHolder(View view) {
            this.circleImage = (ImageView) view.findViewById(R.id.circleImage);
            this.user_name = (TextView) view.findViewById(R.id.user_name);
            this.time = (TextView) view.findViewById(R.id.time);
        }
    }

    private static class VideoHolder {
        VideoView movie;
        ImageView mVideoThumbnail;

        public VideoHolder(View view) {
            this.movie = (VideoView) view.findViewById(R.id.videoView);
            this.mVideoThumbnail = (ImageView) view.findViewById(R.id.video_thumbnail);

        }
    }

    private static class CommentHolder {
        TextView comment;
        TextView likesnumber;
        TextView likes;
        TextView commentsnumber;
        TextView comments;
        TextView sharenumber;
        TextView share;

        public CommentHolder(View view) {
            this.comment = (TextView) view.findViewById(R.id.comment);
            this.likesnumber = (TextView) view.findViewById(R.id.likesnumber);
            this.likes = (TextView) view.findViewById(R.id.likes);
            this.commentsnumber = (TextView) view.findViewById(R.id.commentsnumber);
            this.comments = (TextView) view.findViewById(R.id.comments);
            this.sharenumber = (TextView) view.findViewById(R.id.sharenumber);
            this.share = (TextView) view.findViewById(R.id.share);
        }
    }

    private static class RestHolder {
        ImageView restaurantImage;
        TextView locality;
        TextView rest_name;

        public RestHolder(View view) {
            this.restaurantImage = (ImageView) view.findViewById(R.id.restaurantImage);
            this.rest_name = (TextView) view.findViewById(R.id.rest_name);
            this.locality = (TextView) view.findViewById(R.id.locality);
        }
    }

    private static class LikeCommentHolder {
        ImageView likes;
        ImageView comments;
        ImageView share;

        public LikeCommentHolder(View view) {
            this.likes = (ImageView)view.findViewById(R.id.likes);
            this.comments = (ImageView)view.findViewById(R.id.comments);
            this.share = (ImageView)view.findViewById(R.id.share);

        }
    }

    public class ProfAdapter extends ArrayAdapter<UserData> {
        private LayoutInflater layoutInflater;

        public ProfAdapter(Context context, int viewResourceId, ArrayList<UserData> profusers) {
            super(context, viewResourceId, profusers);
            this.layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            int line = (position / 5) * 5;
            int pos = position - line;

            UserData user = this.getItem(position);

            switch (pos) {
                case 0:
                    convertView = layoutInflater.inflate(R.layout.name_picture_bar, null);
                    break;
                case 1:
                    convertView = layoutInflater.inflate(R.layout.video_bar, null);
                    break;
                case 2:
                    convertView = layoutInflater.inflate(R.layout.comment_bar, null);
                    break;
                case 3:
                    convertView = layoutInflater.inflate(R.layout.restaurant_bar, null);
                    break;
                case 4:
                    convertView = layoutInflater.inflate(R.layout.likes_comments_bar, null);
                    break;
            }

            switch (pos) {
                case 0:
                    NameHolder nameHolder = new NameHolder(convertView);

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
                    videoHolder = new VideoHolder(convertView);

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

                            VideoView nextVideo = (VideoView) mProfListView.findViewWithTag(mShowPosition);

                            if (nextVideo != null) {
                                Log.e("TAG", "pause : " + mShowPosition);
                                nextVideo.stopPlayback();
                            }

                            videoHolder.mVideoThumbnail.setVisibility(View.GONE);
                            videoHolder.movie.start();
                            Log.e("TAG", "start : " + position);
                            mp.setLooping(true);
                            mShowPosition = position;
                        }
                    });
                    videoHolder.movie.setTag(position);
                    }
                    break;

                case 2:
                    CommentHolder commentHolder = new CommentHolder(convertView);
                    commentHolder.likesnumber.setText(user.getgoodnum());
                    commentHolder.commentsnumber.setText(user.getComment_num());
                    break;

                case 3:
                    RestHolder restHolder = new RestHolder(convertView);

                    restHolder.rest_name.setText(user.getRest_name());
                    //restHolder.locality.setText(user.getLocality());
                    break;
                case 4:
                    LikeCommentHolder likeCommentHolder = new LikeCommentHolder(convertView);
                    //クリックされた時の処理
                    break;


            }

            return convertView;
        }
    }
}

