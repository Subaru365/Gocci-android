package com.inase.android.gocci.presenter;

import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.datasource.repository.API3;
import com.inase.android.gocci.domain.model.HeaderData;
import com.inase.android.gocci.domain.model.PostData;
import com.inase.android.gocci.domain.usecase.UserAndRestUseCase;

import java.util.ArrayList;

/**
 * Created by kinagafuji on 15/10/04.
 */
public class ShowRestPagePresenter extends Presenter implements UserAndRestUseCase.UserAndRestUseCaseCallback {
    private UserAndRestUseCase mUserAndRestUseCase;
    private ShowRestView mShowRestView;

    public ShowRestPagePresenter(UserAndRestUseCase userAndRestUseCase) {
        mUserAndRestUseCase = userAndRestUseCase;
    }

    public void setRestView(ShowRestView view) {
        mShowRestView = view;
    }

    public void getRestData(Const.APICategory api, String url) {
        mShowRestView.showLoading();
        mUserAndRestUseCase.execute(api, url, this);
    }

    @Override
    public void onDataLoaded(Const.APICategory api, HeaderData mUserdata, ArrayList<PostData> mPostData, ArrayList<String> post_ids) {
        mShowRestView.hideLoading();
        mShowRestView.hideNoResultCase();
        mShowRestView.showResult(api, mUserdata, mPostData, post_ids);
    }

    @Override
    public void onDataEmpty(Const.APICategory api, HeaderData mUserData) {
        mShowRestView.hideLoading();
        mShowRestView.showNoResultCase(api, mUserData);
    }

    @Override
    public void onCausedByLocalError(Const.APICategory api, String errorMessage) {
        mShowRestView.hideLoading();
        mShowRestView.showNoResultCausedByLocalError(api, errorMessage);
    }

    @Override
    public void onCausedByGlobalError(Const.APICategory api, API3.Util.GlobalCode globalCode) {
        mShowRestView.hideLoading();
        mShowRestView.showNoResultCausedByGlobalError(api, globalCode);
    }

    @Override
    public void initialize() {

    }

    @Override
    public void resume() {
        mUserAndRestUseCase.setCallback(this);
    }

    @Override
    public void pause() {
        mUserAndRestUseCase.removeCallback();
    }

    @Override
    public void destroy() {

    }

    public interface ShowRestView {
        void showLoading();

        void hideLoading();

        void showNoResultCase(Const.APICategory api, HeaderData mRestData);

        void hideNoResultCase();

        void showNoResultCausedByGlobalError(Const.APICategory api, API3.Util.GlobalCode globalCode);

        void showNoResultCausedByLocalError(Const.APICategory api, String errorMessage);

        void showResult(Const.APICategory api, HeaderData mRestData, ArrayList<PostData> mPostData, ArrayList<String> post_ids);
    }
}
