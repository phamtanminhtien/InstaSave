package com.tietha.instasave;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;


public class InstaSaveService extends Service {
    private static final String TAG = "Service" ;
    private ClipboardManager mClipboardManager;
    final static String CHANNEL_ID = "SERVICE";
    private Boolean IS;
    private String URL;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        IS = false;
        mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this, CHANNEL_ID)
                        .setContentTitle("InstaSave Service")
                        .setContentText("InstaSave Service is running!")
                        .setSmallIcon(R.drawable.ic_file_download_white_24dp)
                        .setContentIntent(pendingIntent)
                        .setTicker("Mine")
                        .build();
        startForeground(8769, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mClipboardManager.addPrimaryClipChangedListener(new ClipboardManager.OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                checkData();
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Service";
            String description = "Service is running!";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    private void checkData() {
        if (!(mClipboardManager.hasPrimaryClip())) {

            IS = false;

        } else if (!(mClipboardManager.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {
            IS = false;
        } else {
            URL = mClipboardManager.getPrimaryClip().getItemAt(0).getText().toString();
            IS = URL.lastIndexOf("https://www.instagram.com/") != -1;
        }
        if(IS){
            UpdateUI();
        }
    }

    private void UpdateUI() {
        UrlMedia mUrlImage = new UrlMedia(this, URL);
        final ArrayList<DownloadFile> task = new ArrayList<>();
        mUrlImage.setmOnCallBack(new UrlMedia.OnCallBack() {
            @Override
            public void onResponse(UrlMedia u) {
                task.clear();
                for (int i = 0; i < u.count(); i++){
                    Log.d("AAA", "sadas");
                    task.add(new DownloadFile(InstaSaveService.this));
                    task.get(i).execute(u.getList().get(i));
                }
            }

            @Override
            public void onErrorResponse(String e) {
                Log.d(TAG, e);
            }
        });
    }
}
