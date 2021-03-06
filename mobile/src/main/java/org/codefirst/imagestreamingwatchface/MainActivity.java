package org.codefirst.imagestreamingwatchface;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;

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
    GridView mClockGridView;
    ClockAdapter mClockAdapter;
    String[] mSourceTitleArray;
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

        mSourceTitleArray = getResources().getStringArray(R.array.image_source_values);
        mSourceUrlArray = getResources().getStringArray(R.array.source_url_values);

        mClockGridView = (GridView)findViewById(R.id.gridView);
        mClockAdapter = new ClockAdapter(this, mSourceTitleArray, mSourceUrlArray);
        mClockGridView.setAdapter(mClockAdapter);
        mClockGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (mGoogleApiClient.isConnected()) {
                    final String url = mSourceUrlArray[i];
                    DataAPIUtil.syncAsset(mGoogleApiClient, "/source", "url", url);

                    setTitle(mSourceTitleArray[i]);
                }
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

    private void setTitleFromCache(final DataItem dataItem) {
        DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
        final String url = dataMapItem.getDataMap().getString("url");

        for (int i = 0 ; i < mSourceUrlArray.length; i++) {
            if (mSourceUrlArray[i].equals(url)) {
                setTitle(mSourceTitleArray[i]);
                return;
            }
        }
    }

    @Override
    public void onConnected(final Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (final DataItem dataItem : dataItems) {
                    final String path = dataItem.getUri().getPath();
                    switch(path) {
                        case "/image":
                            setImageFromCache(dataItem);
                            break;
                        case "/source":
                            setTitleFromCache(dataItem);
                            break;
                        default:
                            break;
                    }
                }
                mClockAdapter.notifyDataSetChanged();
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

        // also update thumbnails
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mClockAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Error: connection failed");
    }
}
