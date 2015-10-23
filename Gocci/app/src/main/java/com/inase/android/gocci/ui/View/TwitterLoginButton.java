package com.inase.android.gocci.ui.view;

import android.content.Context;
import android.util.AttributeSet;

import com.inase.android.gocci.R;

public class TwitterLoginButton extends TwitterLoginButton {
    public TwitterLoginButton(Context context) {
        super(context);
        init();
    }

    public TwitterLoginButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TwitterLoginButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        setBackgroundResource(R.drawable.sign_up_button_twitter);
        setTextSize(16);
        setText(getContext().getString(R.string.twitter_login_text));
        setPadding(0, 0, 0, 0);
        setTextColor(getResources().getColor(R.color.custom_text_selected));
        setTypeface(getTypeface());
    }
}