package org.codefirst.imagestreamingwatchface;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class ImageLoaderService extends WearableListenerService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ImageLoaderService";
    private static String SOURCE_URL = "http://www.bijint.com/assets/toppict/jp/pc/%02d%02d.jpg";

    private class ImageLoaderAsyncTask extends AsyncTask<String, Void, Bitmap> {
        protected Asset convertBitmapToAsset(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return Asset.createFromBytes(stream.toByteArray());
        }

        @Override
        protected Bitmap doInBackground(String... url) {
            try {
                InputStream stream = new URL(url[0]).openStream();
                return BitmapFactory.decodeStream(stream);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                DataAPIUtil.syncAsset(mGoogleApiClient, "/image", "image", convertBitmapToAsset(bitmap));
            }
        }
    }

    GoogleApiClient mGoogleApiClient;
    Timer mTimer;

    private void restartImageLoader() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Time time = new Time();
                time.setToNow();
                String url = String.format(SOURCE_URL, time.hour, time.minute);

                Log.d(TAG, "Accessing: " + url);
                (new ImageLoaderAsyncTask()).execute(url);
            }
        }, 0, 30 * 1000);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        restartImageLoader();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mTimer.cancel();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                final String path = event.getDataItem().getUri().getPath();
                if (path.equals("/source")) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    String url = dataMapItem.getDataMap().getString("url");

                    if (url != null) {
                        Log.d(TAG, "Source URL is changed: " + url);
                        SOURCE_URL = url;
                        restartImageLoader();
                    }
                }

            }
        }
    }
}
