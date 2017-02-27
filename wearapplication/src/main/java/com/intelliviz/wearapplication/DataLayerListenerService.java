package com.intelliviz.wearapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.intelliviz.wearapplication.WatchFaceAppService.LOCAL_ACTION2;
import static com.intelliviz.wearapplication.WatchFaceAppService.WEATHER_IMAGE;
import static com.intelliviz.wearapplication.WatchFaceAppService.WEATHER_MAXTEMP_STR;
import static com.intelliviz.wearapplication.WatchFaceAppService.WEATHER_MINTEMP_STR;

public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = DataLayerListenerService.class.getSimpleName();
    private static final int TIMEOUT_MS = 1000;
    private GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : events) {
            final Uri uri = event.getDataItem().getUri();
            final String path = uri != null ? uri.getPath() : null;
            if("/MOBILETOWEAR".equals(path)) {

                final DataMap map = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();

                String mintTempString = map.getString("mintempstring");
                String maxTempString = map.getString("maxtempstring");

                Intent localIntent = new Intent(LOCAL_ACTION2);
                localIntent.putExtra(WEATHER_MINTEMP_STR, mintTempString);
                localIntent.putExtra(WEATHER_MAXTEMP_STR, maxTempString);

                Asset asset = map.getAsset("imageData");
                if(asset != null) {

                    Log.i(TAG, "Adding bitmap data to intent");
                    Bitmap bitmap = loadBitmapFromAsset(asset);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] byteArray = stream.toByteArray();
                    localIntent.putExtra(WEATHER_IMAGE, byteArray);
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            }
        }
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }
        ConnectionResult result =
                mGoogleApiClient.blockingConnect(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();
        mGoogleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}

