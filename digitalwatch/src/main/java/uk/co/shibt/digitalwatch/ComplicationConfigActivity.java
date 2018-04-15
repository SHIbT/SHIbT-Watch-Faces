package uk.co.shibt.digitalwatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;

public class ComplicationConfigActivity extends Activity {

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;
    static final int UPDATE_COLORS_CONFIG_REQUEST_CODE = 1002;
    private static final String TAG = ComplicationConfigActivity.class.getSimpleName();
    private WearableRecyclerView mWearableRecyclerView;
    private ComplicationConfigRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_complication_config);

        mAdapter = new ComplicationConfigRecyclerViewAdapter(
                getApplicationContext(),
                ComplicationConfigData.getWatchFaceServiceClass(),
                ComplicationConfigData.getDataToPopulateAdapter(this));

        mWearableRecyclerView =
                findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        mWearableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mWearableRecyclerView.setHasFixedSize(true);

        mWearableRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE
                && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);

            mAdapter.updateSelectedComplication(complicationProviderInfo);

        } else if (requestCode == UPDATE_COLORS_CONFIG_REQUEST_CODE
                && resultCode == RESULT_OK) {

            mAdapter.updatePreviewColors();
        }
    }
}
