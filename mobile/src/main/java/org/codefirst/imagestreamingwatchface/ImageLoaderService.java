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
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ImageLoaderService extends Service
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    public ImageLoaderService() {
    }

    private class ImageLoaderAsyncTask extends AsyncTask<String, Void, Bitmap> {
        GoogleApiClient mGoogleApiClient;
        public ImageLoaderAsyncTask(GoogleApiClient googleApiClient) {
            mGoogleApiClient = googleApiClient;
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
                Toast.makeText(getApplicationContext(), "downloaded image!", Toast.LENGTH_LONG).show();

                // convert bitmap to byte array
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);

                // store with DataLayer API
                PutDataMapRequest dataMapRequest = PutDataMapRequest.create("/image");
                DataMap dataMap = dataMapRequest.getDataMap();
                dataMap.putByteArray("image", stream.toByteArray());

                PutDataRequest request = dataMapRequest.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(mGoogleApiClient, request);

                Toast.makeText(getApplicationContext(), "image store requested!", Toast.LENGTH_LONG).show();

                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d("TAG", "onResult: " + dataItemResult.getStatus());

                        Toast.makeText(getApplicationContext(), "image stored!", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    GoogleApiClient mGoogleApiClient;
    ImageLoaderAsyncTask mImageLoaderAsyncTask;

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

        Time time = new Time();
        time.setToNow();
        String url = String.format("http://www.bijint.com/assets/toppict/2013jp/t1/%02d%02d.jpg", time.hour, time.minute);
        mImageLoaderAsyncTask = new ImageLoaderAsyncTask(mGoogleApiClient);
        mImageLoaderAsyncTask.execute(url);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d("TAG", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("TAG", "onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("TAG", "onConnectionFailed: " + connectionResult);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
