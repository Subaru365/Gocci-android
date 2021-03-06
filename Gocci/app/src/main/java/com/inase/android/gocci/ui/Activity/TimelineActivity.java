package com.inase.android.gocci.ui.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.andexert.library.RippleView;
import com.inase.android.gocci.R;
import com.inase.android.gocci.consts.Const;
import com.inase.android.gocci.event.BusHolder;
import com.inase.android.gocci.event.FilterTimelineEvent;
import com.inase.android.gocci.event.NotificationNumberEvent;
import com.inase.android.gocci.event.PageChangeVideoStopEvent;
import com.inase.android.gocci.event.TimelineMuteChangeEvent;
import com.inase.android.gocci.ui.fragment.TimelineFollowFragment;
import com.inase.android.gocci.ui.fragment.TimelineLatestFragment;
import com.inase.android.gocci.ui.fragment.TimelineNearFragment;
import com.inase.android.gocci.ui.view.DrawerProfHeader;
import com.inase.android.gocci.ui.view.GochiLayout;
import com.inase.android.gocci.ui.view.NotificationListView;
import com.inase.android.gocci.ui.view.ToukouPopup;
import com.inase.android.gocci.utils.SavedData;
import com.inase.android.gocci.utils.Util;
import com.konifar.fab_transformation.FabTransformation;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems;
import com.squareup.otto.Subscribe;
import com.weiwangcn.betterspinner.library.material.MaterialBetterSpinner;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.nlopez.smartlocation.OnLocationUpdatedListener;
import io.nlopez.smartlocation.SmartLocation;

public class TimelineActivity extends AppCompatActivity {

    private final TimelineActivity self = this;
    @Bind(R.id.tool_bar)
    Toolbar mToolBar;
    @Bind(R.id.smart_tab)
    SmartTabLayout mSmartTab;
    @Bind(R.id.viewpager)
    ViewPager mViewpager;
    @Bind(R.id.overlay)
    View mOverlay;
    @Bind(R.id.fab)
    FloatingActionButton mFab;
    @Bind(R.id.category_spinner)
    MaterialBetterSpinner mCategorySpinner;
    @Bind(R.id.value_spinner)
    MaterialBetterSpinner mValueSpinner;
    @Bind(R.id.sort_spinner)
    MaterialBetterSpinner mSortSpinner;
    @Bind(R.id.sort_cross)
    ImageView mSortCross;
    @Bind(R.id.filter_ripple)
    RippleView mFilterRipple;
    @Bind(R.id.sheet)
    CardView mSheet;
    @Bind(R.id.coordinator_layout)
    CoordinatorLayout mCoordinatorLayout;
    @Bind(R.id.gochi_layout)
    GochiLayout mGochi;

    private float pointX;
    private float pointY;

    @OnClick(R.id.fab)
    public void click() {
        if (PermissionChecker.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mFab.getVisibility() == View.VISIBLE) {
                FabTransformation.with(mFab).setOverlay(mOverlay).transformTo(mSheet);
            }
            SmartLocation.with(TimelineActivity.this).location().oneFix().start(new OnLocationUpdatedListener() {
                @Override
                public void onLocationUpdated(Location location) {
                    mLongitude = location.getLongitude();
                    mLatitude = location.getLatitude();
                }
            });
        } else {
            Toast.makeText(this, "位置情報の取得が許可されていません", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.overlay)
    public void clickOverlay() {
        if (mFab.getVisibility() != View.VISIBLE) {
            FabTransformation.with(mFab).setOverlay(mOverlay).transformFrom(mSheet);
        }
    }

    private TextView mNotificationNumber;

    public static int mShowPosition = 0;

    public static int mNearSort_id = 1;
    public static int mNearCategory_id = 0;
    public static int mNearValue_id = 0;

    public static int mLatestSort_id = 0;
    public static int mLatestCategory_id = 0;
    public static int mLatestValue_id = 0;

    public static int mFollowSort_id = 0;
    public static int mFollowCategory_id = 0;
    public static int mFollowValue_id = 0;

    public static double mLongitude = 139.745433;
    public static double mLatitude = 35.658581;

    private String mTitle = "現在地";

    private String[] SORT;
    private String[] CATEGORY;
    private String[] VALUE;

    private Drawer result;

    private static MobileAnalyticsManager analytics;

    private FragmentPagerItemAdapter adapter;

    private static Handler sHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TimelineActivity activity
                    = (TimelineActivity) msg.obj;
            switch (msg.what) {
                case Const.INTENT_TO_MYPAGE:
                    MyprofActivity.startMyProfActivity(activity);
                    break;
                case Const.INTENT_TO_ADVICE:
                    Util.setAdviceDialog(activity);
                    break;
                case Const.INTENT_TO_SETTING:
                    SettingActivity.startSettingActivity(activity);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    this.getApplicationContext(),
                    Const.ANALYTICS_ID, //Amazon Mobile Analytics App ID
                    Const.IDENTITY_POOL_ID //Amazon Cognito Identity Pool ID
            );
        } catch (InitializationException ex) {
            Log.e(this.getClass().getName(), "Failed to initialize Amazon Mobile Analytics", ex);
        }

        setContentView(R.layout.activity_timeline);
        ButterKnife.bind(this);

        mToolBar.setTitle(mTitle);
        mToolBar.setSubtitle("エリアを変更する");
        mToolBar.setTitleTextAppearance(TimelineActivity.this, R.style.Toolbar_TitleText);
        mToolBar.setSubtitleTextAppearance(TimelineActivity.this, R.style.Toolbar_SubTitleText);
        mToolBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mShowPosition == 0) {
                    MapSearchActivity.startMapSearchActivity(123, mLongitude, mLatitude, TimelineActivity.this);
                }
            }
        });
        setSupportActionBar(mToolBar);

        result = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolBar)
                .withHeader(new DrawerProfHeader(this))
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(getString(R.string.timeline)).withIcon(GoogleMaterial.Icon.gmd_home).withIdentifier(1).withSelectable(false),
                        new PrimaryDrawerItem().withName(getString(R.string.mypage)).withIcon(GoogleMaterial.Icon.gmd_person).withIdentifier(2).withSelectable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(getString(R.string.send_advice)).withIcon(GoogleMaterial.Icon.gmd_send).withSelectable(false).withIdentifier(3),
                        new PrimaryDrawerItem().withName(SavedData.getSettingMute(this) == 0 ? getString(R.string.setting_support_mute) : getString(R.string.setting_support_unmute)).withIcon(GoogleMaterial.Icon.gmd_volume_mute).withSelectable(false).withIdentifier(5),
                        new PrimaryDrawerItem().withName(getString(R.string.settings)).withIcon(GoogleMaterial.Icon.gmd_settings).withSelectable(false).withIdentifier(4)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int i, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            if (drawerItem.getIdentifier() == 2) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_MYPAGE, 0, 0, TimelineActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 3) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_ADVICE, 0, 0, TimelineActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 4) {
                                Message msg =
                                        sHandler.obtainMessage(Const.INTENT_TO_SETTING, 0, 0, TimelineActivity.this);
                                sHandler.sendMessageDelayed(msg, 500);
                            } else if (drawerItem.getIdentifier() == 5) {
                                switch (SavedData.getSettingMute(TimelineActivity.this)) {
                                    case 0:
                                        BusHolder.get().post(new TimelineMuteChangeEvent(-1));
                                        SavedData.setSettingMute(TimelineActivity.this, -1);
                                        result.updateName(5, new StringHolder(getString(R.string.setting_support_unmute)));
                                        break;
                                    case -1:
                                        BusHolder.get().post(new TimelineMuteChangeEvent(0));
                                        SavedData.setSettingMute(TimelineActivity.this, 0);
                                        result.updateName(5, new StringHolder(getString(R.string.setting_support_mute)));
                                        break;
                                }
                            }
                        }
                        return false;
                    }
                })
                .withSavedInstance(savedInstanceState)
                .build();

        result.setSelection(1);

        adapter = new FragmentPagerItemAdapter(
                getSupportFragmentManager(), FragmentPagerItems.with(this)
                .add(R.string.tab_near, TimelineNearFragment.class)
                .add(R.string.tab_follow, TimelineFollowFragment.class)
                .add(R.string.tab_latest, TimelineLatestFragment.class)
                .create());

        mViewpager.setOffscreenPageLimit(2);
        mViewpager.setAdapter(adapter);

        mSmartTab.setViewPager(mViewpager);
        mSmartTab.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                BusHolder.get().post(new PageChangeVideoStopEvent(position));
                mShowPosition = position;
                switch (mShowPosition) {
                    case 0:
                        mSortSpinner.setVisibility(View.GONE);
                        mSortCross.setVisibility(View.GONE);
                        mSortSpinner.setText(SORT[mNearSort_id]);
                        if (mNearCategory_id != 0)
                            mCategorySpinner.setText(CATEGORY[mNearCategory_id]);
                        if (mNearValue_id != 0) mValueSpinner.setText(VALUE[mNearValue_id]);

                        mToolBar.setTitle(mTitle);
                        mToolBar.setSubtitle("エリアを変更する");
                        mToolBar.setSubtitleTextAppearance(TimelineActivity.this, R.style.Toolbar_SubTitleText);
                        mToolBar.setLogo(null);
                        break;
                    case 1:
                        mSortSpinner.setVisibility(View.VISIBLE);
                        mSortCross.setVisibility(View.VISIBLE);
                        mSortSpinner.setText(SORT[mFollowSort_id]);
                        if (mFollowCategory_id != 0)
                            mCategorySpinner.setText(CATEGORY[mFollowCategory_id]);
                        if (mFollowValue_id != 0) mValueSpinner.setText(VALUE[mFollowValue_id]);

                        mToolBar.setTitle("");
                        mToolBar.setSubtitle("");
                        mToolBar.setLogo(R.drawable.ic_gocci_moji_white45);
                        break;
                    case 2:
                        mSortSpinner.setVisibility(View.VISIBLE);
                        mSortCross.setVisibility(View.VISIBLE);
                        mSortSpinner.setText(SORT[mLatestSort_id]);
                        if (mLatestCategory_id != 0)
                            mCategorySpinner.setText(CATEGORY[mLatestCategory_id]);
                        if (mLatestValue_id != 0) mValueSpinner.setText(VALUE[mLatestValue_id]);

                        mToolBar.setTitle("");
                        mToolBar.setSubtitle("");
                        mToolBar.setLogo(R.drawable.ic_gocci_moji_white45);
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == 2) mFab.hide();
                if (state == 0) mFab.show();
            }
        });

        CATEGORY = getResources().getStringArray(R.array.list_category);
        VALUE = getResources().getStringArray(R.array.list_value);
        SORT = getResources().getStringArray(R.array.list_sort);

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, CATEGORY);
        mCategorySpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (mShowPosition) {
                    case 0:
                        mNearCategory_id = position + 2;
                        break;
                    case 1:
                        mFollowCategory_id = position + 2;
                        break;
                    case 2:
                        mLatestCategory_id = position + 2;
                        break;
                }
            }
        });
        ArrayAdapter<String> valueAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, VALUE);
        mValueSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (mShowPosition) {
                    case 0:
                        mNearValue_id = position + 1;
                        break;
                    case 1:
                        mFollowValue_id = position + 1;
                        break;
                    case 2:
                        mLatestValue_id = position + 1;
                        break;
                }
            }
        });
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, SORT);
        mSortSpinner.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (mShowPosition) {
                    case 1:
                        mFollowSort_id = position;
                        break;
                    case 2:
                        mLatestSort_id = position;
                        break;
                }
            }
        });
        mCategorySpinner.setAdapter(categoryAdapter);
        mValueSpinner.setAdapter(valueAdapter);
        mSortSpinner.setAdapter(sortAdapter);

        mFilterRipple.setOnRippleCompleteListener(new RippleView.OnRippleCompleteListener() {
            @Override
            public void onComplete(RippleView rippleView) {
                FabTransformation.with(mFab).setOverlay(mOverlay).transformFrom(mSheet);
                //Otto currentpageと絞り込みurl
                switch (mShowPosition) {
                    case 0:
                        BusHolder.get().post(new FilterTimelineEvent(mShowPosition, Const.getCustomTimelineAPI(mShowPosition,
                                mNearSort_id, mNearCategory_id, mNearValue_id,
                                mLongitude, mLatitude, 0)));
                        break;
                    case 1:
                        BusHolder.get().post(new FilterTimelineEvent(mShowPosition, Const.getCustomTimelineAPI(mShowPosition,
                                mFollowSort_id, mFollowCategory_id, mFollowValue_id,
                                mLongitude, mLatitude, 0)));
                        break;
                    case 2:
                        BusHolder.get().post(new FilterTimelineEvent(mShowPosition, Const.getCustomTimelineAPI(mShowPosition,
                                mLatestSort_id, mLatestCategory_id, mLatestValue_id,
                                mLongitude, mLatitude, 0)));
                        break;
                }
            }
        });

        mSortSpinner.setVisibility(View.GONE);
        mSortCross.setVisibility(View.GONE);

        mGochi.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, final MotionEvent event) {
                //final float y = Util.getScreenHeightInPx(TimelineActivity.this) - event.getRawY();
                pointX = event.getX();
                pointY = event.getY();
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (analytics != null) {
            analytics.getSessionClient().resumeSession();
        }
        BusHolder.get().register(self);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (analytics != null) {
            analytics.getSessionClient().pauseSession();
            analytics.getEventClient().submitEvents();
        }
        BusHolder.get().unregister(self);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 123:
                if (resultCode == RESULT_OK) {
                    Bundle bundle = data.getExtras();
                    mTitle = bundle.getString("place");
                    mToolBar.setTitle(mTitle);
                    adapter.getPage(0).onActivityResult(requestCode, resultCode, data);
                }
                break;
            default:
                break;
        }
    }


    @Subscribe
    public void subscribe(NotificationNumberEvent event) {
        Snackbar.make(mCoordinatorLayout, event.mMessage, Snackbar.LENGTH_SHORT).show();
        //２1文字で改行っぽい
        if (!event.mMessage.equals(getString(R.string.videoposting_complete))) {
            mNotificationNumber.setVisibility(View.VISIBLE);
            mNotificationNumber.setText(String.valueOf(event.mNotificationNumber));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.bell_notification, menu);
        // お知らせ未読件数バッジ表示
        MenuItem item = menu.findItem(R.id.badge);
        MenuItem cameraitem = menu.findItem(R.id.camera);
        MenuItemCompat.setActionView(item, R.layout.toolbar_notification_icon);
        View view = MenuItemCompat.getActionView(item);
        mNotificationNumber = (TextView) view.findViewById(R.id.notification_number);
        int notifications = SavedData.getNotification(this);

        // バッジの数字を更新。0の場合はバッジを表示させない

        if (notifications == 0) {
            mNotificationNumber.setVisibility(View.INVISIBLE);
        } else {
            mNotificationNumber.setText(String.valueOf(notifications));
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNotificationNumber.setVisibility(View.INVISIBLE);
                SavedData.setNotification(TimelineActivity.this, 0);
                View notification = new NotificationListView(TimelineActivity.this);

                final PopupWindow window = ToukouPopup.newBasicPopupWindow(TimelineActivity.this);

                View header = notification.findViewById(R.id.header_view);
                header.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (window.isShowing()) {
                            window.dismiss();
                        }
                    }
                });
                window.setContentView(notification);
                //int totalHeight = getWindowManager().getDefaultDisplay().getHeight();
                int[] location = new int[2];
                v.getLocationOnScreen(location);
                ToukouPopup.showLikeQuickAction(window, notification, v, TimelineActivity.this.getWindowManager(), 0, 0);
            }
        });

        cameraitem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                enableCamera();
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    private void enableCamera() {
        int requestcode = 40;
        List<String> permissionArray = new ArrayList<>();
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(Manifest.permission.CAMERA);
            requestcode = requestcode + 1;
        }
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(Manifest.permission.RECORD_AUDIO);
            requestcode = requestcode + 1;
        }
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            requestcode = requestcode + 1;
        }
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionArray.add(Manifest.permission.ACCESS_FINE_LOCATION);
            requestcode = requestcode + 1;
        }
        if (requestcode != 40) {
            String[] permissions = new String[permissionArray.size()];
            permissions = permissionArray.toArray(permissions);
            rationaleDialog(permissions, requestcode);
        } else {
            goCamera();
        }
    }

    private void rationaleDialog(final String[] permissions, final int requestCode) {
        new MaterialDialog.Builder(this)
                .title("権限許可のお願い")
                .titleColorRes(R.color.namegrey)
                .content("カメラを起動するには以下のような権限が必要になります。\n\n"
                        + "・カメラ(動画の撮影)\n\n"
                        + "・録音(音声の録音)\n\n"
                        + "・ストレージ(動画の作成)\n\n"
                        + "・位置情報(店舗情報の取得)\n\n"
                        + "権限を許可しますか？")
                .contentColorRes(R.color.nameblack)
                .positiveText("許可する").positiveColorRes(R.color.gocci_header)
                .negativeText("いいえ").negativeColorRes(R.color.gocci_header)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        ActivityCompat.requestPermissions(TimelineActivity.this, permissions, requestCode);
                    }
                })
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                        Toast.makeText(TimelineActivity.this, "カメラは起動できませんでした...", Toast.LENGTH_SHORT).show();
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 44:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (grantResults.length > 0 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[2] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[3] == PackageManager.PERMISSION_GRANTED) {
                        goCamera();
                    } else {
                        rationaleSettingDialog();
                    }
                } else {
                    checkCamera();
                }
                break;
            case 43:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (grantResults.length > 0 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                        goCamera();
                    } else {
                        rationaleSettingDialog();
                    }
                } else {
                    checkCamera();
                }
                break;
            case 42:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (grantResults.length > 0 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        goCamera();
                    } else {
                        rationaleSettingDialog();
                    }
                } else {
                    checkCamera();
                }
                break;
            case 41:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (grantResults.length > 0 &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        goCamera();
                    } else {
                        rationaleSettingDialog();
                    }
                } else {
                    checkCamera();
                }
                break;
            case 38:
            case 39:
                adapter.getPage(0).onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
        // other 'case' lines to check for other
        // permissions this app might request
    }

    private void rationaleSettingDialog() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO) ||
                !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            new MaterialDialog.Builder(this)
                    .title("権限許可のお願い")
                    .titleColorRes(R.color.namegrey)
                    .content("カメラを起動するには権限を許可する必要があるため、設定を変更する必要があります")
                    .contentColorRes(R.color.nameblack)
                    .positiveText("変更する")
                    .positiveColorRes(R.color.gocci_header)
                    .negativeText("いいえ")
                    .negativeColorRes(R.color.gocci_header)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null); //Fragmentの場合はgetContext().getPackageName()
                            intent.setData(uri);
                            startActivity(intent);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                            Toast.makeText(TimelineActivity.this, "カメラは起動できませんでした", Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        } else {
            Toast.makeText(TimelineActivity.this, "カメラは起動できませんでした", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkCamera() {
        if (PermissionChecker.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED &&
                PermissionChecker.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                PermissionChecker.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            goCamera();
        } else {
            Toast.makeText(TimelineActivity.this, "カメラは起動できませんでした", Toast.LENGTH_SHORT).show();
        }
    }

    private void goCamera() {
        if (SavedData.getVideoUrl(TimelineActivity.this).equals("") || SavedData.getLat(TimelineActivity.this) == 0.0) {
            startActivity(new Intent(TimelineActivity.this, CameraActivity.class));
        } else {
            new MaterialDialog.Builder(TimelineActivity.this)
                    .title(getString(R.string.already_exist_video))
                    .titleColorRes(R.color.namegrey)
                    .content(getString(R.string.already_exist_video_message))
                    .contentColorRes(R.color.namegrey)
                    .positiveText(getString(R.string.already_exist_video_yeah))
                    .positiveColorRes(R.color.gocci_header)
                    .negativeText(getString(R.string.already_exist_video_no))
                    .negativeColorRes(R.color.gocci_header)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                            Intent intent = new Intent(TimelineActivity.this, CameraPreviewAlreadyExistActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                            SharedPreferences prefs = getSharedPreferences("movie", Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.clear();
                            editor.apply();
                            startActivity(new Intent(TimelineActivity.this, CameraActivity.class));
                        }
                    }).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else if (mFab.getVisibility() != View.VISIBLE) {
            FabTransformation.with(mFab).setOverlay(mOverlay).transformFrom(mSheet);
        } else {
            super.onBackPressed();
            overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
        }
    }

    public void setNowLocationTitle() {
        mTitle = "現在地";
        mToolBar.setTitle(mTitle);
    }

    public void setGochiLayout() {
        final float y = Util.getScreenHeightInPx(this) - pointY;
        mGochi.post(new Runnable() {
            @Override
            public void run() {
                mGochi.addGochi(R.drawable.ic_icon_beef_orange, pointX, y);
            }
        });
    }
}
