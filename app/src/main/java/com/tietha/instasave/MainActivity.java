package com.tietha.instasave;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "INSTASAVEe";
    private static final int MY_PERMISSIONS_REQUEST_WRITE_STORAGE = 1000;
    public static final String CHANNEL_ID = "DOWNLOADLIST";
    private Switch mSwitch;
    private Intent mService;
    private ArrayList<Bitmap> mList;
    private RecyclerView recyclerView;
    private AdapterListMain adapter;
    private BroadcastReceiver mRefreshReceiver;
    private ClipboardManager mClipboardManager;
    private String URL;
    private Boolean IS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IS = false;
        mClipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        Boolean result = checkPermission();
        if (result) {
            checkFolder();
        }
        if (!isConnectingToInternet(this)) {
            Toast.makeText(this, "Please Connect to Internet", Toast.LENGTH_LONG).show();
        } else {
            init();
            createNotificationChannel();
            mSwitch = findViewById(R.id.switch_start_service);
            if (isMyServiceRunning(InstaSaveService.class)) {
                mSwitch.setChecked(true);
            } else {
                mSwitch.setChecked(false);
            }
            mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @RequiresApi(api = Build.VERSION_CODES.O)
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    if (b) {
                        startServiceI();
                        checkData();
                    } else {
                        stopServiceI();
                    }
                }
            });


            recyclerView = findViewById(R.id.recyclerView);
            mList = new ArrayList<>();
            File file = new File(Environment.getExternalStorageDirectory(), "InstaSave");
            if (file.isDirectory()) {
                File[] listFile = file.listFiles();
                Arrays.sort(listFile);
                for (File value : listFile) {
                    if (checkType(value) == UrlMedia.TYPE_IMG) {
                        mList.add(BitmapFactory.decodeFile(value.getPath()));
                    } else {
                        mList.add(ThumbnailUtils.createVideoThumbnail(value.getPath(),
                                MediaStore.Images.Thumbnails.MINI_KIND));
                    }
                }
            }
            adapter = new AdapterListMain(mList, this);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(this, RecyclerView.HORIZONTAL, true);
            recyclerView.setLayoutManager(mLayoutManager);
            recyclerView.setAdapter(adapter);
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            recyclerView.setNestedScrollingEnabled(false);
            //
            IntentFilter filter = new IntentFilter();
            filter.addAction("update");
            mRefreshReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (Objects.equals(intent.getAction(), "update")) {
                        update();
                    }
                }
            };
            LocalBroadcastManager.getInstance(this).registerReceiver(mRefreshReceiver, filter);

        }

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
                    task.add(new DownloadFile(MainActivity.this));
                    task.get(i).execute(u.getList().get(i));
                }
            }

            @Override
            public void onErrorResponse(String e) {
                Log.d(TAG, e);
            }
        });
    }
    private void update() {
        mList.clear();
        File file = new File(Environment.getExternalStorageDirectory(), "InstaSave");
        if (file.isDirectory()) {
            File[] listFile = file.listFiles();
            Arrays.sort(listFile);
            for (File value : listFile) {
                if (checkType(value) == UrlMedia.TYPE_IMG) {
                    mList.add(BitmapFactory.decodeFile(value.getPath()));
                } else {
                    mList.add(ThumbnailUtils.createVideoThumbnail(value.getPath(),
                            MediaStore.Images.Thumbnails.MINI_KIND));
                }
            }
        }
        adapter.notifyDataSetChanged();
        recyclerView.smoothScrollToPosition(adapter.getItemCount() - 1);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRefreshReceiver);
    }

    private int checkType(File f) {
        if (!f.isFile()) return -1;
        String name = f.getName();
        String[] split = name.split("\\.");
        String type = split[split.length - 1];
        if (type.equals("jpg")) {
            return UrlMedia.TYPE_IMG;
        } else {
            return UrlMedia.TYPE_VIDEO;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startServiceI() {
        startService(mService);
    }

    private void stopServiceI() {
        stopService(mService);
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Downloaded";
            String description = "Action when downloaded";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public static boolean isConnectingToInternet(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void init() {
        mService = new Intent(MainActivity.this, InstaSaveService.class);
    }


    public boolean checkPermission() {
        int currentAPIVersion = Build.VERSION.SDK_INT;
        if (currentAPIVersion >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertBuilder.setCancelable(true);
                    alertBuilder.setTitle("Permission necessary");
                    alertBuilder.setMessage("Write Storage permission is necessary to Download Images and Videos!!!");
                    alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                        }
                    });
                    AlertDialog alert = alertBuilder.create();
                    alert.show();
                } else {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }


    public void checkAgain() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(MainActivity.this);
            alertBuilder.setCancelable(true);
            alertBuilder.setTitle("Permission necessary");
            alertBuilder.setMessage("Write Storage permission is necessary to Download Images and Videos!!!");
            alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
                }
            });
            AlertDialog alert = alertBuilder.create();
            alert.show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_WRITE_STORAGE);
        }
    }


    //Here you can check App Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_STORAGE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkFolder();
                } else {
                    //code for deny
                    checkAgain();
                }
                break;
        }
    }

    public void checkFolder() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/InstaSave/";
        File dir = new File(path);
        boolean isDirectoryCreated = dir.exists();
        if (!isDirectoryCreated) {
            isDirectoryCreated = dir.mkdir();
        }
        if (isDirectoryCreated) {
            // do something\
            Log.d("Folder", "Already Created");
        }
    }
}
