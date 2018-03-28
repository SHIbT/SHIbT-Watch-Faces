package uk.co.shibt.shibtsimpleanaloguewatchface;

/**
 * Created by Tom Branton on 12/03/2018.
 */

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.complications.ProviderUpdateRequester;

public class ComplicationToggleReceiver extends BroadcastReceiver {
    static final int MAX_NUMBER = 20;
    static final String COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY =
            "uk.co.shibt.shibtsimpleanaloguewatchface.COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY";
    private static final String EXTRA_PROVIDER_COMPONENT =
            "uk.co.shibt.shibtsimpleanaloguewatchface.action.PROVIDER_COMPONENT";
    private static final String EXTRA_COMPLICATION_ID =
            "uk.co.shibt.shibtsimpleanaloguewatchface.action.COMPLICATION_ID";

    /**
     * Returns a pending intent, suitable for use as a tap intent, that causes a complication to be
     * toggled and updated.
     */
    static PendingIntent getToggleIntent(
            Context context, ComponentName provider, int complicationId) {
        Intent intent = new Intent(context, ComplicationToggleReceiver.class);
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, provider);
        intent.putExtra(EXTRA_COMPLICATION_ID, complicationId);

        // Pass complicationId as the requestCode to ensure that different complications get
        // different intents.
        return PendingIntent.getBroadcast(
                context, complicationId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * Returns the key for the shared preference used to hold the current state of a given
     * complication.
     */
    static String getPreferenceKey(ComponentName provider, int complicationId) {
        return provider.getClassName() + complicationId;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        ComponentName provider = extras.getParcelable(EXTRA_PROVIDER_COMPONENT);
        int complicationId = extras.getInt(EXTRA_COMPLICATION_ID);

        String preferenceKey = getPreferenceKey(provider, complicationId);
        SharedPreferences sharedPreferences =
                context.getSharedPreferences(COMPLICATION_PROVIDER_PREFERENCES_FILE_KEY, 0);

        int value = sharedPreferences.getInt(preferenceKey, 0);

        // Updates data for complication.
        value = (value + 1) % MAX_NUMBER;

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(preferenceKey, value);
        editor.apply();

        // Request an update for the complication that has just been toggled.
        ProviderUpdateRequester requester = new ProviderUpdateRequester(context, provider);
        requester.requestUpdate(complicationId);
    }
}
