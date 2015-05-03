package org.codefirst.imagestreamingwatchface;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

public class DataAPIUtil {
    private static final String TAG = "DataAPIUtil";

    protected static Asset convertBitmapToAsset(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return Asset.createFromBytes(stream.toByteArray());
    }

    protected static void sync(GoogleApiClient googleApiClient, PutDataMapRequest dataMapRequest) {
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "onResult: " + dataItemResult.getStatus());
            }
        });
    }

    // todo: rename syncAsset to syncWatchFaceImage
    public static void syncAsset(GoogleApiClient googleApiClient, String path, String key, Bitmap bitmap) {
        Asset asset = convertBitmapToAsset(bitmap);

        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
        DataMap dataMap = dataMapRequest.getDataMap();
        dataMap.putAsset(key, asset);

        sync(googleApiClient, dataMapRequest);
    }

    // todo: rename syncAsset to syncSourceUrl
    public static void syncAsset(GoogleApiClient googleApiClient, String path, String key, String str) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
        DataMap dataMap = dataMapRequest.getDataMap();
        dataMap.putString(key, str);

        sync(googleApiClient, dataMapRequest);
    }
}
