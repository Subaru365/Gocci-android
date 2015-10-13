package com.inase.android.gocci.datasource.repository;

import com.inase.android.gocci.Application_Gocci;
import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.domain.model.PostData;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;

/**
 * Created by kinagafuji on 15/09/25.
 */
public class PostDataRepositoryImpl implements PostDataRepository {
    private static PostDataRepositoryImpl sPostDataRepository;

    public PostDataRepositoryImpl() {
    }

    public static PostDataRepositoryImpl getRepository() {
        if (sPostDataRepository == null) {
            sPostDataRepository = new PostDataRepositoryImpl();
        }
        return sPostDataRepository;
    }

    @Override
    public void getPostDataList(final int api, String url, final PostDataRepositoryCallback cb) {
        final ArrayList<PostData> mPostData = new ArrayList<>();
        final ArrayList<String> mPost_Ids = new ArrayList<>();
        Application_Gocci.getJsonSyncHttpClient(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                try {
                    if (response.length() != 0) {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject jsonObject = response.getJSONObject(i);
                            //mPostData.add(PostData.createPostData(jsonObject));
                            mPostData.add(PostData.createDistPostData(jsonObject));
                            mPost_Ids.add(jsonObject.getString("post_id"));
                        }
                        cb.onPostDataLoaded(api, mPostData, mPost_Ids);
                    } else {
                        if (api == Const.TIMELINE_ADD) {
                            cb.onPostDataLoaded(api, mPostData, mPost_Ids);
                        } else {
                            cb.onPostDataEmpty(api);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                cb.onError();
            }
        });
    }
}
