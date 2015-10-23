package com.inase.android.gocci.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.andexert.library.RippleView;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.inase.android.gocci.Application_Gocci;
import com.inase.android.gocci.R;
import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.event.BusHolder;
import com.inase.android.gocci.event.SNSMatchFinishEvent;
import com.inase.android.gocci.ui.activity.TimelineActivity;
import com.inase.android.gocci.ui.view.TwitterLoginButton;
import com.squareup.otto.Subscribe;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by kinagafuji on 15/08/05.
 */
public class LoginSocialAuthenticationFragment extends Fragment {

    @Bind(R.id.login_button)
    LoginButton mFacebookLoginButton;
    @Bind(R.id.twitter_login_button)
    TwitterLoginButton mTwitterLoginButton;
    @Bind(R.id.skip_Ripple)
    RippleView mSkipRipple;

    @OnClick(R.id.twitter_ripple)
    public void twitter() {
        if (Application_Gocci.getLoginProvider() != null) {
            new MaterialDialog.Builder(getActivity()).
                    content(getString(R.string.add_sns_message))
                    .positiveText(getString(R.string.add_sns_yeah))
                    .positiveColorRes(R.color.gocci_header)
                    .negativeText(getString(R.string.add_sns_no))
                    .negativeColorRes(R.color.gocci_header)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                            mTwitterLoginButton.performClick();
                        }
                    }).show();
        } else {
            Toast.makeText(getActivity(), getString(R.string.please_input_username), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.facebook_ripple)
    public void facebook() {
        if (Application_Gocci.getLoginProvider() != null) {
            new MaterialDialog.Builder(getActivity()).
                    content(getString(R.string.add_sns_message))
                    .positiveText(getString(R.string.add_sns_yeah))
                    .positiveColorRes(R.color.gocci_header)
                    .negativeText(getString(R.string.add_sns_no))
                    .negativeColorRes(R.color.gocci_header)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                            if (AccessToken.getCurrentAccessToken() != null) {
                                LoginManager.getInstance().logOut();
                            }
                            mFacebookLoginButton.performClick();
                        }
                    }).show();
        } else {
            Toast.makeText(getActivity(), getString(R.string.please_input_username), Toast.LENGTH_SHORT).show();
        }
    }

    private CallbackManager callbackManager;

    public static LoginSocialAuthenticationFragment newInstance() {
        LoginSocialAuthenticationFragment pane = new LoginSocialAuthenticationFragment();
        return pane;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.view_tutorial5, container, false);
        ButterKnife.bind(this, rootView);

        callbackManager = CallbackManager.Factory.create();

        mFacebookLoginButton.setReadPermissions("public_profile");
        mFacebookLoginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Toast.makeText(getActivity(), getString(R.string.preparing_authorize), Toast.LENGTH_SHORT).show();
                Profile profile = Profile.getCurrentProfile();
                String profile_img = "https://graph.facebook.com/" + profile.getId() + "/picture";
                Application_Gocci.addLogins(getActivity(), Const.ENDPOINT_FACEBOOK, AccessToken.getCurrentAccessToken().getToken(), profile_img);

                    /*
                    Intent intent = new Intent(getActivity(), GocciTimelineActivity.class);
                    startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                    getActivity().finish();
                    */
            }

            @Override
            public void onCancel() {
                Toast.makeText(getActivity(), getString(R.string.cancel_login), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException e) {
                Toast.makeText(getActivity(), getString(R.string.error_login), Toast.LENGTH_SHORT).show();
            }
        });

        mTwitterLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                Toast.makeText(getActivity(), getString(R.string.preparing_authorize), Toast.LENGTH_SHORT).show();
                TwitterAuthToken authToken = result.data.getAuthToken();

                String username = result.data.getUserName();
                String profile_img = "http://www.paper-glasses.com/api/twipi/" + username;
                Application_Gocci.addLogins(getActivity(), "api.twitter.com", authToken.token + ";" + authToken.secret, profile_img);

                    /*
                    Intent intent = new Intent(getActivity(), GocciTimelineActivity.class);
                    startActivity(intent);
                    getActivity().overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                    getActivity().finish();
                    */
            }

            @Override
            public void failure(TwitterException exception) {
                // Do something on failure
                Toast.makeText(getActivity(), getString(R.string.error_login), Toast.LENGTH_SHORT).show();
            }
        });

        mSkipRipple.setOnRippleCompleteListener(new RippleView.OnRippleCompleteListener() {
            @Override
            public void onComplete(RippleView rippleView) {
                if (Application_Gocci.getLoginProvider() != null) {
                    goTimeline(0);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.please_input_username), Toast.LENGTH_SHORT).show();
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        BusHolder.get().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BusHolder.get().unregister(this);
    }

    @Subscribe
    public void subscribe(SNSMatchFinishEvent event) {
        goTimeline(0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mTwitterLoginButton.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void goTimeline(int time) {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getActivity(), TimelineActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                getActivity().finish();
            }
        }, time);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ButterKnife.unbind(this);
    }
}