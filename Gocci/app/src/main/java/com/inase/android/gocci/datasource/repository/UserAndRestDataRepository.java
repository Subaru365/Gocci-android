package com.inase.android.gocci.datasource.repository;

import com.inase.android.gocci.data.HeaderData;
import com.inase.android.gocci.data.PostData;

import java.util.ArrayList;

/**
 * Created by kinagafuji on 15/09/29.
 */
public interface UserAndRestDataRepository {
    void getUserDataList(int api, String url, UserAndRestDataRepositoryCallback cb);
    void getRestDataList(int api, String url, UserAndRestDataRepositoryCallback cb);

    interface UserAndRestDataRepositoryCallback {
        void onUserAndRestDataLoaded(int api, HeaderData userData, ArrayList<PostData> postData);

        void onUserAndRestDataEmpty(int api, HeaderData userData);

        void onError();
    }
}