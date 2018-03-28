package uk.co.shibt.shibtsimpleanaloguewatchface;

/**
 * Created by SHIbT on 13/03/2018.
 */

import android.content.Context;
import android.graphics.Color;

import java.util.ArrayList;

public class ComplicationConfigData {

    public static Class getWatchFaceServiceClass() {
        return SimpleAnalogueWatchFace.class;
    }

    public static ArrayList<Integer> getColorOptionsDataSet() {
        ArrayList<Integer> colorOptionsDataSet = new ArrayList<>();
        colorOptionsDataSet.add(Color.parseColor("#FFFFFF")); // White
        colorOptionsDataSet.add(Color.parseColor("#FFEB3B")); // Yellow
        colorOptionsDataSet.add(Color.parseColor("#FFC107")); // Amber
        colorOptionsDataSet.add(Color.parseColor("#FF9800")); // Orange
        colorOptionsDataSet.add(Color.parseColor("#FF5722")); // Deep Orange
        colorOptionsDataSet.add(Color.parseColor("#F44336")); // Red
        colorOptionsDataSet.add(Color.parseColor("#E91E63")); // Pink
        colorOptionsDataSet.add(Color.parseColor("#9C27B0")); // Purple
        colorOptionsDataSet.add(Color.parseColor("#673AB7")); // Deep Purple
        colorOptionsDataSet.add(Color.parseColor("#3F51B5")); // Indigo
        colorOptionsDataSet.add(Color.parseColor("#2196F3")); // Blue
        colorOptionsDataSet.add(Color.parseColor("#03A9F4")); // Light Blue
        colorOptionsDataSet.add(Color.parseColor("#00BCD4")); // Cyan
        colorOptionsDataSet.add(Color.parseColor("#009688")); // Teal
        colorOptionsDataSet.add(Color.parseColor("#4CAF50")); // Green
        colorOptionsDataSet.add(Color.parseColor("#8BC34A")); // Lime Green
        colorOptionsDataSet.add(Color.parseColor("#CDDC39")); // Lime
        colorOptionsDataSet.add(Color.parseColor("#607D8B")); // Blue Grey
        colorOptionsDataSet.add(Color.parseColor("#9E9E9E")); // Grey
        colorOptionsDataSet.add(Color.parseColor("#795548")); // Brown
        colorOptionsDataSet.add(Color.parseColor("#000000")); // Black
        return colorOptionsDataSet;
    }

    public static ArrayList<ConfigItemType> getDataToPopulateAdapter(Context context) {

        ArrayList<ConfigItemType> settingsConfigData = new ArrayList<>();

        // Data for watch face preview and complications UX in settings Activity.
        ConfigItemType complicationConfigItem =
                new PreviewAndComplicationsConfigItem(R.drawable.add_complication);
        settingsConfigData.add(complicationConfigItem);

        // Data for "more options" UX in settings Activity.
        ConfigItemType moreOptionsConfigItem =
                new MoreOptionsConfigItem(R.drawable.ic_expand_more_white_18dp);
        settingsConfigData.add(moreOptionsConfigItem);

        // Data for highlight/marker (second hand) color UX in settings Activity.
        ConfigItemType markerColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_marker_color_label),
                        R.drawable.icn_styles,
                        context.getString(R.string.saved_marker_color),
                        ColorSelectionActivity.class);
        settingsConfigData.add(markerColorConfigItem);

        // Data for Background color UX in settings Activity.
        ConfigItemType backgroundColorConfigItem =
                new ColorConfigItem(
                        context.getString(R.string.config_background_color_label),
                        R.drawable.icn_styles,
                        context.getString(R.string.saved_background_color),
                        ColorSelectionActivity.class);
        settingsConfigData.add(backgroundColorConfigItem);

        return settingsConfigData;
    }

    public interface ConfigItemType {
        int getConfigType();
    }

    public static class PreviewAndComplicationsConfigItem implements ConfigItemType {

        private int defaultComplicationResourceId;

        PreviewAndComplicationsConfigItem(int defaultComplicationResourceId) {
            this.defaultComplicationResourceId = defaultComplicationResourceId;
        }

        public int getDefaultComplicationResourceId() {
            return defaultComplicationResourceId;
        }

        @Override
        public int getConfigType() {
            return ComplicationConfigRecyclerViewAdapter.TYPE_PREVIEW_AND_COMPLICATIONS_CONFIG;
        }
    }

    public static class MoreOptionsConfigItem implements ConfigItemType {

        private int iconResourceId;

        MoreOptionsConfigItem(int iconResourceId) {
            this.iconResourceId = iconResourceId;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        @Override
        public int getConfigType() {
            return ComplicationConfigRecyclerViewAdapter.TYPE_MORE_OPTIONS;
        }
    }

    public static class ColorConfigItem implements ConfigItemType {

        private String name;
        private int iconResourceId;
        private String sharedPrefString;
        private Class<ColorSelectionActivity> activityToChoosePreference;

        ColorConfigItem(
                String name,
                int iconResourceId,
                String sharedPrefString,
                Class<ColorSelectionActivity> activity) {
            this.name = name;
            this.iconResourceId = iconResourceId;
            this.sharedPrefString = sharedPrefString;
            this.activityToChoosePreference = activity;
        }

        public String getName() {
            return name;
        }

        public int getIconResourceId() {
            return iconResourceId;
        }

        public String getSharedPrefString() {
            return sharedPrefString;
        }

        public Class<ColorSelectionActivity> getActivityToChoosePreference() {
            return activityToChoosePreference;
        }

        @Override
        public int getConfigType() {
            return ComplicationConfigRecyclerViewAdapter.TYPE_COLOR_CONFIG;
        }
    }

}
