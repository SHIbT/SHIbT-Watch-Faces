package uk.co.shibt.shibtsimpleanaloguewatchface;

/**
 * Created by Tom Branton on 12/03/2018.
 */

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;

import uk.co.shibt.shibtsimpleanaloguewatchface.R;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigData;

public class ColorSelectionActivity extends Activity{

    static final String EXTRA_SHARED_PREF =
            "uk.co.shibt.shibtsimpleanaloguewatchface.EXTRA_SHARED_PREF";
    private static final String TAG = ColorSelectionActivity.class.getSimpleName();
    private WearableRecyclerView mConfigAppearanceWearableRecyclerView;

    private ColorSelectionRecyclerViewAdapter mColorSelectionRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_color_selection_config);

        // Assigns SharedPreference String used to save color selected.
        String sharedPrefString = getIntent().getStringExtra(EXTRA_SHARED_PREF);

        mColorSelectionRecyclerViewAdapter = new ColorSelectionRecyclerViewAdapter(
                sharedPrefString,
                ComplicationConfigData.getColorOptionsDataSet());

        mConfigAppearanceWearableRecyclerView =
                findViewById(R.id.wearable_recycler_view);

        // Aligns the first and last items on the list vertically centered on the screen.
        mConfigAppearanceWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        mConfigAppearanceWearableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mConfigAppearanceWearableRecyclerView.setHasFixedSize(true);

        mConfigAppearanceWearableRecyclerView.setAdapter(mColorSelectionRecyclerViewAdapter);
    }
}
