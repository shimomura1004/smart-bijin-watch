package org.codefirst.imagestreamingwatchface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;


public class MainActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener {

    private static final String TAG = "MainActivity";

    GoogleApiClient mGoogleApiClient;
    Spinner mImageSourceSpinner;
    String[] mSourceUrlArray;

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

        startService(new Intent(MainActivity.this, ImageLoaderService.class));

        mImageSourceSpinner = (Spinner)findViewById(R.id.image_source_spinner);
        mSourceUrlArray = getResources().getStringArray(R.array.source_url_values);

        mImageSourceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (mGoogleApiClient.isConnected()) {
                    final String url = mSourceUrlArray[i];
                    DataAPIUtil.syncAsset(mGoogleApiClient, "/source", "url", url);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopService(new Intent(MainActivity.this, ImageLoaderService.class));
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

    private void setImageFromCache(final DataItem dataItem) {
        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
        Asset image = dataMapItem.getDataMap().getAsset("image");

        Wearable.DataApi.getFdForAsset(mGoogleApiClient, image).setResultCallback(new ResultCallback<DataApi.GetFdForAssetResult>() {
            @Override
            public void onResult(DataApi.GetFdForAssetResult getFdForAssetResult) {
                InputStream assetInputStream = getFdForAssetResult.getInputStream();
                final Bitmap bitmap = BitmapFactory.decodeStream(assetInputStream);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = (ImageView) findViewById(R.id.imageView);
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (final DataItem dataItem : dataItems) {
                    if (dataItem.getUri().getPath().equals("/image")) {
                        setImageFromCache(dataItem);
                    }
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Wearable.DataApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                Asset image = dataMapItem.getDataMap().getAsset("image");

                if (image == null) {
                    continue;
                }

                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(mGoogleApiClient, image).await().getInputStream();
                final Bitmap bitmap = BitmapFactory.decodeStream(assetInputStream);

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
        Log.d(TAG, "Error: connection failed");
    }
}
