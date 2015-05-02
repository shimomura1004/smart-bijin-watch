package org.codefirst.imagestreamingwatchface;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class ImageLoaderService extends Service
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "ImageLoaderService";
    private static final String SOURCE_URL = "http://www.bijint.com/assets/toppict/jp/pc/%02d%02d.jpg";

    private class ImageLoaderAsyncTask extends AsyncTask<String, Void, Bitmap> {
        protected Asset convertBitmapToAsset(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            return Asset.createFromBytes(stream.toByteArray());
        }

        protected void syncAsset(String path, String key, Asset asset) {
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
            DataMap dataMap = dataMapRequest.getDataMap();
            dataMap.putAsset(key, asset);

            PutDataRequest request = dataMapRequest.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, request);

            pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    Log.d(TAG, "onResult: " + dataItemResult.getStatus());
                }
            });
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
                syncAsset("/image", "image", convertBitmapToAsset(bitmap));
            }
        }
    }

    GoogleApiClient mGoogleApiClient;
    Timer mTimer;

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
    public void onDestroy() {
        super.onDestroy();

        mTimer.cancel();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "onConnectionFailed: " + connectionResult);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
