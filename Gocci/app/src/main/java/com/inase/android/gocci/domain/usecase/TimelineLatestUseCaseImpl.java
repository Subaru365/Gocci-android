package com.inase.android.gocci.domain.usecase;

import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.datasource.repository.API3;
import com.inase.android.gocci.datasource.repository.PostDataRepository;
import com.inase.android.gocci.domain.executor.PostExecutionThread;
import com.inase.android.gocci.domain.model.TwoCellData;

import java.util.ArrayList;

/**
 * Created by kinagafuji on 15/09/25.
 */
public class TimelineLatestUseCaseImpl extends UseCase2<Const.APICategory, String> implements TimelineLatestUseCase, PostDataRepository.PostDataRepositoryCallback {
    private static TimelineLatestUseCaseImpl sUseCase;
    private final PostDataRepository mPostDataRepository;
    private PostExecutionThread mPostExecutionThread;
    private LatestTimelineUseCaseCallback mCallback;

    public static TimelineLatestUseCaseImpl getUseCase(PostDataRepository postDataRepository, PostExecutionThread postExecutionThread) {
        if (sUseCase == null) {
            sUseCase = new TimelineLatestUseCaseImpl(postDataRepository, postExecutionThread);
        }
        return sUseCase;
    }

    public TimelineLatestUseCaseImpl(PostDataRepository postDataRepository, PostExecutionThread postExecutionThread) {
        mPostDataRepository = postDataRepository;
        mPostExecutionThread = postExecutionThread;
    }

    @Override
    public void execute(Const.APICategory api, String url, LatestTimelineUseCaseCallback callback) {
        mCallback = callback;
        this.start(api, url);
    }

    @Override
    protected void call(Const.APICategory param1, String param2) {
        mPostDataRepository.getPostDataList(param1, param2, this);
    }

    @Override
    public void setCallback(LatestTimelineUseCaseCallback callback) {
        mCallback = callback;
    }

    @Override
    public void removeCallback() {
        mCallback = null;
    }

    @Override
    public void onPostDataLoaded(final Const.APICategory api, final ArrayList<TwoCellData> postData, final ArrayList<String> post_ids) {
        mPostExecutionThread.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onLatestTimelineLoaded(api, postData, post_ids);
                }
            }
        });
    }

    @Override
    public void onPostDataEmpty(final Const.APICategory api) {
        mPostExecutionThread.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onLatestTimelineEmpty(api);
                }
            }
        });
    }

    @Override
    public void onCausedByLocalError(final Const.APICategory api, final String errorMessage) {
        mPostExecutionThread.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onCausedByLocalError(api, errorMessage);
                }
            }
        });
    }

    @Override
    public void onCausedByGlobalError(final Const.APICategory api, final API3.Util.GlobalCode globalCode) {
        mPostExecutionThread.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    mCallback.onCausedByGlobalError(api, globalCode);
                }
            }
        });
    }
}
