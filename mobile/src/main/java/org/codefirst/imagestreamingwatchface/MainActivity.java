package org.codefirst.imagestreamingwatchface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;


public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener {

    private class ImageLoaderAsyncTask extends AsyncTask<String, Void, Bitmap> {
        public ImageLoaderAsyncTask() {}

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
                ImageView imageView = (ImageView)findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

//        startService(new Intent(MainActivity.this, ImageLoaderService.class));
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mGoogleApiClient.disconnect();
        stopService(new Intent(MainActivity.this, ImageLoaderService.class));
        Log.d("TAG", "Stopped service");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(getApplicationContext(), "connected!", Toast.LENGTH_LONG).show();
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        startService(new Intent(MainActivity.this, ImageLoaderService.class));
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d("TAG", "data changed!!!");

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d("TAG", "DataItem deleted: " + event.getDataItem().getUri());
            }
            else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d("TAG", "DataItem changed: " + event.getDataItem().getUri());

                DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
                byte[] data = dataMap.getByteArray("image");
                final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, new BitmapFactory.Options());

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = (ImageView) findViewById(R.id.imageView);
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    public void onReloadButtonClicked(View view) {
        ImageLoaderAsyncTask imageLoaderAsyncTask = new ImageLoaderAsyncTask();

        Time time = new Time();
        time.setToNow();
        String url = String.format("http://www.bijint.com/assets/toppict/jp/pc/%02d%02d.jpg", time.hour, time.minute);
        imageLoaderAsyncTask.execute(url);
    }
}
