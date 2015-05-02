package org.codefirst.imagestreamingwatchface;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class DataAPIUtil {
    private static final String TAG = "DataAPIUtil";

    protected static void sync(GoogleApiClient googleApiClient, PutDataMapRequest dataMapRequest) {
        PutDataRequest request = dataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(googleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "onResult: " + dataItemResult.getStatus());
            }
        });
    }

    public static void syncAsset(GoogleApiClient googleApiClient, String path, String key, Asset asset) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
        DataMap dataMap = dataMapRequest.getDataMap();
        dataMap.putAsset(key, asset);

        sync(googleApiClient, dataMapRequest);
    }

    public static void syncAsset(GoogleApiClient googleApiClient, String path, String key, String str) {
        PutDataMapRequest dataMapRequest = PutDataMapRequest.create(path);
        DataMap dataMap = dataMapRequest.getDataMap();
        dataMap.putString(key, str);

        sync(googleApiClient, dataMapRequest);
    }
}
