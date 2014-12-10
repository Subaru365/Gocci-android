package com.example.kinagafuji.gocci.Activity;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.example.kinagafuji.gocci.Fragment.LoginFragment;

public class LoginActivity2 extends FragmentActivity {

    private LoginFragment loginFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Add the fragment on initial activity setup
            loginFragment = new LoginFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, loginFragment)
                    .commit();
        } else {
            // Or set the fragment from restored state info
            loginFragment = (LoginFragment) getSupportFragmentManager()
                    .findFragmentById(android.R.id.content);
        }

    }

}