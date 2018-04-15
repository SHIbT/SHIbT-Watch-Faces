package uk.co.shibt.digitalwatch;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.view.CircledImageView;
import android.support.wear.widget.RoundedDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class ColorSelectionRecyclerViewAdapter extends
        RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = ColorSelectionRecyclerViewAdapter.class.getSimpleName();

    private ArrayList<Integer> mColorOptionsDataSet;
    private String mSharedPrefString;

    public ColorSelectionRecyclerViewAdapter(
            String sharedPrefString,
            ArrayList<Integer> colorSettingsDataSet) {

        mSharedPrefString = sharedPrefString;
        mColorOptionsDataSet = colorSettingsDataSet;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder viewHolder =
                new ColorViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.color_config_list_item, parent, false));
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {

        Integer color = mColorOptionsDataSet.get(position);
        ColorViewHolder colorViewHolder = (ColorViewHolder) viewHolder;
        colorViewHolder.setColor(color);
    }

    @Override
    public int getItemCount() {
        return mColorOptionsDataSet.size();
    }

    public class ColorViewHolder extends RecyclerView.ViewHolder
            implements View.OnClickListener {

        private CircledImageView mColorCircleImageView;

        public ColorViewHolder(final View view) {
            super(view);
            mColorCircleImageView = view.findViewById(R.id.color);
            view.setOnClickListener(this);
        }

        public void setColor(int color) {
            mColorCircleImageView.setCircleColor(color);
        }

        @Override
        public void onClick(View view) {
            int position = getAdapterPosition();
            Integer color = mColorOptionsDataSet.get(position);

            Activity activity = (Activity) view.getContext();

            if (mSharedPrefString != null && !mSharedPrefString.isEmpty()) {
                SharedPreferences sharedPref = activity.getSharedPreferences(
                        activity.getString(R.string.complication_preference_file_key),
                        Context.MODE_PRIVATE);

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt(mSharedPrefString, color);
                editor.apply();

                // Let's Complication Config Activity know there was an update to colors.
                activity.setResult(Activity.RESULT_OK);
            }
            activity.finish();
        }
    }
}