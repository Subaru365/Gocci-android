package com.inase.android.gocci.domain.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by kinagafuji on 15/11/16.
 */
public class TwoCellData {

    private static final String TAG_POST_ID = "post_id";
    private static final String TAG_MOVIE = "movie";
    private static final String TAG_THUMBNAIL = "thumbnail";
    private static final String TAG_POST_REST_ID = "rest_id";
    private static final String TAG_RESTNAME = "restname";
    private static final String TAG_POST_USER_ID = "user_id";
    private static final String TAG_USERNAME = "username";
    private static final String TAG_CHEER_FLAG = "cheer_flag";
    private static final String TAG_GOCHI_FLAG = "gochi_flag";
    private static final String TAG_POST_DATE = "post_date";
    private static final String TAG_MP4_MOVIE = "mp4_movie";
    private static final String TAG_HLS_MOVIE = "hls_movie";

    private static final String TAG_DISTANCE = "distance";

    private String post_id;
    private String movie;
    private String thumbnail;
    private String post_rest_id;
    private String restname;
    private String post_user_id;
    private String username;
    private int cheer_flag;
    private int gochi_flag;
    private String post_date;
    private String mp4_movie;
    private String hls_movie;

    private int distance;

    public TwoCellData() {
    }

    public TwoCellData(String post_id, String movie, String thumbnail, String post_rest_id, String restname,
                       String post_user_id, String username, int cheer_flag, int gochi_flag, String post_date, String mp4_movie, String hls_movie,
                       int distance) {
        this.post_id = post_id;
        this.movie = movie;
        this.thumbnail = thumbnail;
        this.post_rest_id = post_rest_id;
        this.restname = restname;
        this.post_user_id = post_user_id;
        this.username = username;
        this.cheer_flag = cheer_flag;
        this.gochi_flag = gochi_flag;
        this.post_date = post_date;
        this.mp4_movie = mp4_movie;
        this.hls_movie = hls_movie;
        this.distance = distance;
    }

    public TwoCellData(String post_id, String movie, String thumbnail, String post_rest_id, String restname,
                       String post_user_id, String username, int cheer_flag, int gochi_flag, String post_date, String mp4_movie, String hls_movie) {
        this.post_id = post_id;
        this.movie = movie;
        this.thumbnail = thumbnail;
        this.post_rest_id = post_rest_id;
        this.restname = restname;
        this.post_user_id = post_user_id;
        this.username = username;
        this.cheer_flag = cheer_flag;
        this.gochi_flag = gochi_flag;
        this.post_date = post_date;
        this.mp4_movie = mp4_movie;
        this.hls_movie = hls_movie;
    }

    public static TwoCellData createPostData(JSONObject jsonObject) {
        try {
            String post_id = jsonObject.getString(TAG_POST_ID);
            String movie = jsonObject.getString(TAG_MOVIE);
            String thumbnail = jsonObject.getString(TAG_THUMBNAIL);
            String post_rest_id = jsonObject.getString(TAG_POST_REST_ID);
            String restname = jsonObject.getString(TAG_RESTNAME);
            String post_user_id = jsonObject.getString(TAG_POST_USER_ID);
            String username = jsonObject.getString(TAG_USERNAME);
            int cheer_flag = jsonObject.getInt(TAG_CHEER_FLAG);
            int gochi_flag = jsonObject.getInt(TAG_GOCHI_FLAG);
            String post_date = jsonObject.getString(TAG_POST_DATE);
            String mp4_movie = jsonObject.getString(TAG_MP4_MOVIE);
            String hls_movie = jsonObject.getString(TAG_HLS_MOVIE);

            if (jsonObject.has(TAG_DISTANCE)) {
                int distance = jsonObject.getInt(TAG_DISTANCE);
                return new TwoCellData(post_id, movie, thumbnail, post_rest_id, restname,
                        username, post_user_id, cheer_flag, gochi_flag, post_date, mp4_movie, hls_movie, distance);
            } else {
                return new TwoCellData(post_id, movie, thumbnail, post_rest_id, restname,
                        post_user_id, username, cheer_flag, gochi_flag, post_date, mp4_movie, hls_movie);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPost_id() {
        return post_id;
    }

    public void setPost_id(String post_id) {
        this.post_id = post_id;
    }

    public String getMovie() {
        return movie;
    }

    public void setMovie(String movie) {
        this.movie = movie;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getPost_rest_id() {
        return post_rest_id;
    }

    public void setPost_rest_id(String post_rest_id) {
        this.post_rest_id = post_rest_id;
    }

    public String getRestname() {
        return restname;
    }

    public void setRestname(String restname) {
        this.restname = restname;
    }

    public String getPost_user_id() {
        return post_user_id;
    }

    public void setPost_user_id(String post_user_id) {
        this.post_user_id = post_user_id;
    }

    public int getCheer_flag() {
        return cheer_flag;
    }

    public void setCheer_flag(int cheer_flag) {
        this.cheer_flag = cheer_flag;
    }

    public String getPost_date() {
        return post_date;
    }

    public void setPost_date(String post_date) {
        this.post_date = post_date;
    }

    public String getMp4_movie() {
        return mp4_movie;
    }

    public void setMp4_movie(String mp4_movie) {
        this.mp4_movie = mp4_movie;
    }

    public String getHls_movie() {
        return hls_movie;
    }

    public void setHls_movie(String hls_movie) {
        this.hls_movie = hls_movie;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getGochi_flag() {
        return gochi_flag;
    }

    public void setGochi_flag(int gochi_flag) {
        this.gochi_flag = gochi_flag;
    }
}
