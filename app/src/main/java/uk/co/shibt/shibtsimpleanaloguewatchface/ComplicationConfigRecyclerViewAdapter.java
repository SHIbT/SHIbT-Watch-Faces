package uk.co.shibt.shibtsimpleanaloguewatchface;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderInfoRetriever;
import android.support.wearable.complications.ProviderInfoRetriever.OnProviderInfoReceivedCallback;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import uk.co.shibt.shibtsimpleanaloguewatchface.R;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigData.BackgroundComplicationConfigItem;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigData.ColorConfigItem;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigData.ConfigItemType;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigData.MoreOptionsConfigItem;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigData.PreviewAndComplicationsConfigItem;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigData.UnreadNotificationConfigItem;
import uk.co.shibt.shibtsimpleanaloguewatchface.SimpleAnalogueWatchFace.SimpleAnalogueWatchFace;

import java.util.ArrayList;
import java.util.concurrent.Executors;
/**
 * Created by SHIbT on 13/03/2018.
 */

public class ComplicationConfigRecyclerViewAdapter {
}
