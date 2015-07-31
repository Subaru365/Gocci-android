package com.inase.android.gocci.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.inase.android.gocci.Camera.down18CameraFragment;
import com.inase.android.gocci.Camera.up18CameraFragment;
import com.inase.android.gocci.R;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class GocciCameraActivity extends AppCompatActivity {

    public static ArrayList<String> restname = new ArrayList<>();
    public static ArrayList<Integer> rest_id = new ArrayList<>();
    protected static final int REQUEST_CHECK_SETTINGS = 0x1;

    public static boolean isLocationOnOff = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gocci_camera);

        if (savedInstanceState == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new up18CameraFragment()).commit();
            } else {
                getFragmentManager().beginTransaction()
                        .add(R.id.container, new down18CameraFragment()).commit();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // Check for the integer request code originally supplied to startResolutionForResult().
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        Log.e("ログ", "User agreed to make required location settings changes.");
                        //firstLocation();
                        isLocationOnOff = true;
                        break;
                    case Activity.RESULT_CANCELED:
                        Log.e("ログ", "User chose not to make required location settings changes.");
                        //ダイアログをキャンセルした
                        Toast.makeText(this, "位置情報機能が取れないので、カメラを終了します", Toast.LENGTH_LONG).show();
                        isLocationOnOff = false;
                        finish();
                        break;
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        new MaterialDialog.Builder(this)
                .title("確認")
                .content("すでに録画中の場合、その動画は初期化されますがよろしいですか？")
                .positiveText("戻る")
                .negativeText("いいえ")
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        super.onPositive(dialog);
                        GocciCameraActivity.this.finish();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                    }
                }).show();
    }

}
