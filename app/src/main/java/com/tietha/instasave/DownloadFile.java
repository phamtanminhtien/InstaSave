package com.tietha.instasave;


import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class DownloadFile extends AsyncTask<String, Integer, String> {

    static int id;
    private Context mainCtx;
    private String path;

    DownloadFile(Context context){
        this.mainCtx = context;
        id = 0;
    }

    private String getType(String url){
        String cutGetParams = url.split("\\?")[0];
        String[] splitType = cutGetParams.split("\\.");
        return splitType[splitType.length - 1];

    }
    @Override
    protected String doInBackground(String... sUrl) {
        InputStream input = null;
        OutputStream output = null;
        HttpURLConnection connection = null;
        try {
            java.net.URL url = new URL(sUrl[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return "Server returned HTTP " + connection.getResponseCode()
                        + " " + connection.getResponseMessage();
            }

            int fileLength = connection.getContentLength();

            input = connection.getInputStream();
            String fileN = "ISGDownloader_" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 10) + "." + getType(sUrl[0]);
            File filename = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                    "/InstaSave", fileN);
            path = filename.getPath();
            output = new FileOutputStream(filename);

            byte data[] = new byte[4096];
            long total = 0;
            int count;
            while ((count = input.read(data)) != -1) {
                if (isCancelled()) {
                    input.close();
                    return null;
                }
                total += count;
                if (fileLength > 0)
                    publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }
            new SingleMediaScanner(mainCtx, filename);
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (output != null)
                    output.close();
                if (input != null)
                    input.close();
            } catch (IOException ignored) {
            }

            if (connection != null)
                connection.disconnect();
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onPostExecute(String s) {
        if( s == null){
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mainCtx, MainActivity.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_file_download_white_24dp)
                    .setContentTitle("Downloaded!")
                    .setContentText("Downloaded! Click to show media")
                    .setLargeIcon(BitmapFactory.decodeFile(path))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setStyle(new NotificationCompat.BigPictureStyle()
                            .bigPicture(BitmapFactory.decodeFile(path))
                            .bigLargeIcon(null))
                    .setColor(mainCtx.getColor(R.color.colorNotification));
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(mainCtx);

// notificationId is a unique int for each notification that you must define
            notificationManager.notify(id, mBuilder.build());
            id++;
            Intent bc = new Intent();
            bc.setAction("update");
            LocalBroadcastManager.getInstance(mainCtx).sendBroadcast(bc);
        }

    }
}
