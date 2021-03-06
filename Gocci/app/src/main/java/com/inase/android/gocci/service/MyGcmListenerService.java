package com.inase.android.gocci.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;
import com.inase.android.gocci.R;
import com.inase.android.gocci.event.BusHolder;
import com.inase.android.gocci.event.NotificationNumberEvent;
import com.inase.android.gocci.ui.activity.SplashActivity;
import com.inase.android.gocci.utils.SavedData;

import java.util.Arrays;

/**
 * Created by kinagafuji on 15/08/12.
 */
public class MyGcmListenerService extends GcmListenerService {
    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String def = data.getString("default");
        Log.e(TAG, "From: " + from);
        Integer[] list = SavedData.getSettingNotifications(this);
        if (def != null) {
            if (def.matches(".*" + getString(R.string.notice_from_gochi) + ".*")) {
                if (Arrays.asList(list).contains(0)) {
                    sendNotification(def);
                }
            } else if (def.matches(".*" + getString(R.string.notice_from_comment) + ".*")) {
                if (Arrays.asList(list).contains(1)) {
                    sendNotification(def);
                }
            } else if (def.matches(".*" + getString(R.string.notice_from_follow) + ".*")) {
                if (Arrays.asList(list).contains(2)) {
                    sendNotification(def);
                }
            }

            if (!def.equals(getString(R.string.videoposting_complete))) {
                int badge_num = SavedData.getNotification(getApplicationContext());
                BusHolder.get().post(new NotificationNumberEvent(badge_num + 1, def));
                SavedData.setNotification(getApplicationContext(), badge_num + 1);
            } else {
                int badge_num = SavedData.getNotification(getApplicationContext());
                BusHolder.get().post(new NotificationNumberEvent(badge_num, def));
            }
        }
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param msg GCM message received.
     */
    private void sendNotification(String msg) {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_gocci_push)
                .setContentTitle(getString(R.string.info_gocci))
                .setContentText(msg)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }
}
