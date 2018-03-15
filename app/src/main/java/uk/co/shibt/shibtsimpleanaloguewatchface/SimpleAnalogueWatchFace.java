package uk.co.shibt.shibtsimpleanaloguewatchface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.v7.graphics.Palette;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.widget.Toast;
import uk.co.shibt.shibtsimpleanaloguewatchface.R;
import uk.co.shibt.shibtsimpleanaloguewatchface.ComplicationConfigRecyclerViewAdapter;

import static android.graphics.Typeface.MONOSPACE;
import static android.graphics.Typeface.SANS_SERIF;

import java.text.SimpleDateFormat;
import java.lang.ref.WeakReference;
import java.lang.String;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SimpleAnalogueWatchFace extends CanvasWatchFaceService {

    private static final String TAG = "SimpleAnalogueWatchFace";

    // Unique IDs for each complication. The settings activity that supports allowing users
    // to select their complication data provider requires numbers to be >= 0.
    private static final int BACKGROUND_COMPLICATION_ID = 0;

    private static final int TOP_COMPLICATION_ID = 100;
    private static final int BOTTOM_COMPLICATION_ID = 101;
    private static final int LEFT_COMPLICATION_ID = 102;

    // Background, Left and right complication IDs as array for Complication API.
    private static final int[] COMPLICATION_IDS = {
            BACKGROUND_COMPLICATION_ID, TOP_COMPLICATION_ID, BOTTOM_COMPLICATION_ID, LEFT_COMPLICATION_ID
    };

    // Left and right dial supported types.
    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_LARGE_IMAGE},
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            },
            {
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_SHORT_TEXT,
                    ComplicationData.TYPE_SMALL_IMAGE
            }
    };
    // Used by {@link ComplicationConfigRecyclerViewAdapter} to check if complication location
    // is supported in settings config activity.
    public static int getComplicationId(
            ComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case BACKGROUND:
                return BACKGROUND_COMPLICATION_ID;
            case TOP:
                return TOP_COMPLICATION_ID;
            case LEFT:
                return LEFT_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    // Used by {@link ComplicationConfigRecyclerViewAdapter} to retrieve all complication ids.
    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    // Used by {@link ComplicationConfigRecyclerViewAdapter} to see which complication types
    // are supported in the settings config activity.
    public static int[] getSupportedComplicationTypes(
            ComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        // Add any other supported locations here.
        switch (complicationLocation) {
            case BACKGROUND:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case TOP:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case LEFT:
                return COMPLICATION_SUPPORTED_TYPES[2];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[3];
            default:
                return new int[] {};
        }
    }

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<SimpleAnalogueWatchFace.Engine> mWeakReference;

        public EngineHandler(SimpleAnalogueWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        private static final int MSG_UPDATE_TIME = 0;

        @Override
        public void handleMessage(Message msg) {
            SimpleAnalogueWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        /* Setup of the hands size */
        private static final int MSG_UPDATE_TIME = 0;
        private static final float HOUR_STROKE_WIDTH = 5f;
        private static final float MINUTE_STROKE_WIDTH = 3f;
        private static final float SECOND_TICK_STROKE_WIDTH = 2f;
        private static final float HOUR_CIRCLE_STROKE_WIDTH = 4f;
        private static final float SECOND_CIRCLE_STROKE_WIDTH = 2f;
        private static final float OUTER_TICK_CIRCLE_STROKE_WIDTH = 1f;

        private static final float HOUR_TICK_STROKE_WIDTH = 2f;
        private static final float MINUTE_TICK_STROKE_WIDTH = 1f;

        private static final float CENTER_GAP_AND_CIRCLE_RADIUS = 4f;
        private static final int SHADOW_RADIUS = 7;

        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode;
        private float mCenterX;
        private float mCenterY;
        private float mTextWidth;
        private float mWidth;
        private float mSecondHandLength;
        private float sMinuteHandLength;
        private float sHourHandLength;

        /* Colors for all hands (hour, minute, seconds, ticks) based on photo loaded. */
//        private int mWatchHandColor;
        private int mWatchHourMinuteColor;
        private int mWatchSecondColor;
//        private int mWatchHandHighlightColor;
        private int mWatchInnerSecondCircleColor;
        private int mWatchHandShadowColor;
        private int mWatchInnerCircleColor;
//        private int mWatchOuterCircleColor;
        private int mWatchHourTickColor;
        private int mWatchMinuteTickColor;
        private int mBackgroundPaintColor;
        private int mWatchWhiteColor;
        private int mWatchOuterCircleColor;

        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mTickAndCirclePaint;
        private Paint mHourTickPaint;
        private Paint mMinuteTickPaint; /* Also covers seconds paint */
        private Paint mInnerCirclePaint;
        private Paint mInnerRedCirclePaint;
        private Paint mOuterCirclePaint;
        private Paint mBackgroundPaint;
        private Paint mTextPaint;
        private Paint mTextPaintHours;
        private Paint mDateBoxPaint;

        private Bitmap mBackgroundBitmap;
        private Bitmap mGrayBackgroundBitmap;

        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;

        SharedPreferences mSharedPref;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SimpleAnalogueWatchFace.this)
                    .setAcceptsTapEvents(true)
                    .setStatusBarGravity(Gravity.CENTER_VERTICAL)
                    .build());

            mCalendar = Calendar.getInstance();

            /* Set defaults for colors */
            mWatchWhiteColor = getColor(R.color.wl_White);
            mWatchHourMinuteColor = getColor(R.color.wl_White);
            mWatchSecondColor = getColor(R.color.wl_Red);
//            mWatchHandHighlightColor = getColor(R.color.wl_Red);
            mWatchHandShadowColor = getColor(R.color.wl_DarkGrey);
            mWatchInnerCircleColor = getColor(R.color.wl_White);
            mWatchInnerSecondCircleColor = getColor(R.color.wl_Red);
            mWatchOuterCircleColor = getColor(R.color.wl_White);
            mWatchHourTickColor = getColor(R.color.wl_White);
            mWatchMinuteTickColor = getColor(R.color.wl_White);
            mBackgroundPaintColor = getColor(R.color.wl_Black);

            loadSavedPreferences();
            initializeBackground();
            initializeComplications();
            initializeWatchFace();
        }
        private void loadSavedPreferences() {

            /*String backgroundColorResourceName =
                    getApplicationContext().getString(R.string.saved_background_color);

            mBackgroundPaintColor = mSharedPref.getInt(backgroundColorResourceName, Color.BLACK);

            String markerColorResourceName =
                    getApplicationContext().getString(R.string.saved_marker_color);

            // Set defaults for colors
            mWatchHourMinuteColor = mSharedPref.getInt(markerColorResourceName, Color.RED);*/

            if (mBackgroundPaintColor == Color.WHITE) {
                mWatchHourMinuteColor = Color.BLACK;
                mWatchHandShadowColor = Color.WHITE;
            } else {
                mWatchHourMinuteColor = Color.WHITE;
                mWatchHandShadowColor = Color.BLACK;
            }

/*            String unreadNotificationPreferenceResourceName =
                    getApplicationContext().getString(R.strings.saved_unread_notifications_pref);*/

//            mUnreadNotificationsPreference =
//                    mSharedPref.getBoolean(unreadNotificationPreferenceResourceName, true);
        }

        private void initializeBackground() {
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundPaintColor);

            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bg);

//            /* Extracts colors from background image to improve watchface style. */
//            Palette.from(mBackgroundBitmap).generate(new Palette.PaletteAsyncListener() {
//                @Override
//                public void onGenerated(Palette palette) {
//                    if (palette != null) {
//                        mWatchSecondColor = palette.getVibrantColor(Color.RED);
//                        mWatchHourMinuteColor = palette.getLightVibrantColor(Color.WHITE);
//                        mWatchHandShadowColor = palette.getDarkMutedColor(Color.DKGRAY);
//                        updateWatchHandStyle();
//                    }
//                }
//            });
        }
        private void initializeComplications() {

            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            // Creates a ComplicationDrawable for each location where the user can render a
            // complication on the watch face. In this watch face, we create one for left, right,
            // and background, but you could add many more.
            ComplicationDrawable topComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable bottomComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable leftComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            ComplicationDrawable backgroundComplicationDrawable =
                    new ComplicationDrawable(getApplicationContext());

            // Adds new complications to a SparseArray to simplify setting styles and ambient
            // properties for all complications, i.e., iterate over them all.
            mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

            mComplicationDrawableSparseArray.put(TOP_COMPLICATION_ID, topComplicationDrawable);
            mComplicationDrawableSparseArray.put(BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);
            mComplicationDrawableSparseArray.put(LEFT_COMPLICATION_ID, leftComplicationDrawable);
            mComplicationDrawableSparseArray.put(
                    BACKGROUND_COMPLICATION_ID, backgroundComplicationDrawable);

            setComplicationsActiveAndAmbientColors(mWatchHourTickColor);
            setActiveComplications(COMPLICATION_IDS);
        }

        private void initializeWatchFace() {

            mHourPaint = new Paint();
            mHourPaint.setColor(mWatchHourMinuteColor);
            mHourPaint.setStrokeWidth(HOUR_STROKE_WIDTH);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);
            mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mWatchHourMinuteColor);
            mMinutePaint.setStrokeWidth(MINUTE_STROKE_WIDTH);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);
            mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mWatchSecondColor);
            mSecondPaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mHourTickPaint = new Paint();
            mHourTickPaint.setColor(mWatchHourTickColor);
            mHourTickPaint.setStrokeWidth(HOUR_TICK_STROKE_WIDTH);
            mHourTickPaint.setAntiAlias(true);
            mHourTickPaint.setStyle(Paint.Style.STROKE);
            mHourTickPaint.setStrokeCap(Paint.Cap.SQUARE);
            mHourTickPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mMinuteTickPaint = new Paint();
            mMinuteTickPaint.setColor(mWatchMinuteTickColor);
            mMinuteTickPaint.setStrokeWidth(MINUTE_TICK_STROKE_WIDTH);
            mMinuteTickPaint.setAntiAlias(true);
            mMinuteTickPaint.setStyle(Paint.Style.STROKE);
            mMinuteTickPaint.setStrokeCap(Paint.Cap.SQUARE);
            mMinuteTickPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mInnerCirclePaint = new Paint();
            mInnerCirclePaint.setColor(mWatchInnerCircleColor);
            mInnerCirclePaint.setStrokeWidth(HOUR_CIRCLE_STROKE_WIDTH);
            mInnerCirclePaint.setAntiAlias(true);
            mInnerCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mInnerCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mInnerRedCirclePaint = new Paint();
            mInnerRedCirclePaint.setColor(mWatchInnerSecondCircleColor);
            mInnerRedCirclePaint.setStrokeWidth(SECOND_CIRCLE_STROKE_WIDTH);
            mInnerRedCirclePaint.setAntiAlias(true);
            mInnerRedCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mInnerRedCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mOuterCirclePaint = new Paint();
            mOuterCirclePaint.setColor(mWatchOuterCircleColor);
            mOuterCirclePaint.setStrokeWidth(OUTER_TICK_CIRCLE_STROKE_WIDTH);
            mOuterCirclePaint.setAntiAlias(true);
            mOuterCirclePaint.setStyle(Paint.Style.STROKE);
            mOuterCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTickAndCirclePaint = new Paint();
            mTickAndCirclePaint.setColor(mWatchMinuteTickColor);
            mTickAndCirclePaint.setStrokeWidth(SECOND_TICK_STROKE_WIDTH);
            mTickAndCirclePaint.setAntiAlias(true);
            mTickAndCirclePaint.setStyle(Paint.Style.STROKE);
            mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);

            mTextPaint = new Paint();
            mTextPaint.setTypeface(MONOSPACE);
            mTextPaint.setTextSize(28f);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextAlign(Paint.Align.LEFT);
            mTextPaint.setColor(mWatchHourMinuteColor);

            mTextPaintHours = new Paint();
            mTextPaintHours.setTypeface(SANS_SERIF);
            mTextPaintHours.setTextSize(20f);
            mTextPaintHours.setAntiAlias(true);
            mTextPaintHours.setTextAlign(Paint.Align.CENTER);
            mTextPaintHours.setColor(mWatchHourMinuteColor);

            mDateBoxPaint = new Paint();
            mDateBoxPaint.setColor(mWatchHourMinuteColor);
            mDateBoxPaint.setStrokeWidth(SECOND_CIRCLE_STROKE_WIDTH);
            mDateBoxPaint.setAntiAlias(true);
            mDateBoxPaint.setStyle(Paint.Style.STROKE);
            mDateBoxPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
        }
        private void setComplicationsActiveAndAmbientColors(int primaryComplicationColor) {
            int complicationId;
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationId = COMPLICATION_IDS[i];
                complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                if (complicationId == BACKGROUND_COMPLICATION_ID) {
                    // It helps for the background color to be black in case the image used for the
                    // watch face's background takes some time to load.
                    complicationDrawable.setBackgroundColorActive(Color.BLACK);
                } else {
                    // Active mode colors.
                    complicationDrawable.setBorderColorActive(primaryComplicationColor);
                    complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor);

                    // Ambient mode colors.
                    complicationDrawable.setBorderColorAmbient(Color.WHITE);
                    complicationDrawable.setRangedValuePrimaryColorAmbient(Color.WHITE);
                }
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);

            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);

                complicationDrawable.setLowBitAmbient(mLowBitAmbient);
                complicationDrawable.setBurnInProtection(mBurnInProtection);
            }
        }
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {

            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

            // Updates correct ComplicationDrawable with updated data.
            ComplicationDrawable complicationDrawable =
                    mComplicationDrawableSparseArray.get(complicationId);
            complicationDrawable.setComplicationData(complicationData);

            invalidate();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;
            updateWatchHandStyle();

            // Update drawable complications' ambient state.
            // Note: ComplicationDrawable handles switching between active/ambient colors, we just
            // have to inform it to enter ambient mode.
            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
                complicationDrawable.setInAmbientMode(mAmbient);
            }
             /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(mWatchHandShadowColor);
                mMinutePaint.setColor(mWatchHandShadowColor);
                mSecondPaint.setColor(mWatchHandShadowColor);
                mHourTickPaint.setColor(mWatchHandShadowColor);
                mMinuteTickPaint.setColor(mWatchHandShadowColor);
                mInnerCirclePaint.setColor(mWatchHandShadowColor);
                mInnerRedCirclePaint.setColor(mWatchHandShadowColor);
                mOuterCirclePaint.setColor(mWatchHandShadowColor);
                mTickAndCirclePaint.setColor(mWatchHandShadowColor);
                mTextPaint.setColor(mWatchHandShadowColor);
                mDateBoxPaint.setColor(mWatchHandShadowColor);

                mHourPaint.setAntiAlias(false);
                mMinutePaint.setAntiAlias(false);
                mSecondPaint.setAntiAlias(false);
                mHourTickPaint.setAntiAlias(false);
                mMinuteTickPaint.setAntiAlias(false);
                mInnerCirclePaint.setAntiAlias(false);
                mInnerRedCirclePaint.setAntiAlias(false);
                mOuterCirclePaint.setAntiAlias(false);
                mTickAndCirclePaint.setAntiAlias(false);
                mTextPaint.setAntiAlias(false);
                mDateBoxPaint.setAntiAlias(false);

                mHourPaint.clearShadowLayer();
                mMinutePaint.clearShadowLayer();
                mSecondPaint.clearShadowLayer();
                mHourTickPaint.clearShadowLayer();
                mMinuteTickPaint.clearShadowLayer();
                mInnerCirclePaint.clearShadowLayer();
                mInnerRedCirclePaint.clearShadowLayer();
                mOuterCirclePaint.clearShadowLayer();
                mTickAndCirclePaint.clearShadowLayer();
                mDateBoxPaint.clearShadowLayer();

            } else {
                mHourPaint.setColor(mWatchHourMinuteColor);
                mMinutePaint.setColor(mWatchHourMinuteColor);
                mSecondPaint.setColor(mWatchSecondColor);
                mHourTickPaint.setColor(mWatchHourTickColor);
                mMinuteTickPaint.setColor(mWatchMinuteTickColor);
                mInnerCirclePaint.setColor(mWatchInnerCircleColor);
                mInnerRedCirclePaint.setColor(mWatchInnerSecondCircleColor);
                mOuterCirclePaint.setColor(mWatchOuterCircleColor);
                mTickAndCirclePaint.setColor(mWatchMinuteTickColor);
                mTextPaint.setColor(mWatchHourMinuteColor);
                mDateBoxPaint.setColor(mWatchHourMinuteColor);

                mHourPaint.setAntiAlias(true);
                mMinutePaint.setAntiAlias(true);
                mSecondPaint.setAntiAlias(true);
                mHourTickPaint.setAntiAlias(true);
                mMinuteTickPaint.setAntiAlias(true);
                mInnerCirclePaint.setAntiAlias(true);
                mInnerRedCirclePaint.setAntiAlias(true);
                mOuterCirclePaint.setAntiAlias(true);
                mTickAndCirclePaint.setAntiAlias(true);
                mTextPaint.setAntiAlias(true);
                mDateBoxPaint.setAntiAlias(true);

                mHourPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinutePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mSecondPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mHourTickPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mMinuteTickPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mInnerCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mInnerRedCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mOuterCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTickAndCirclePaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mTextPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
                mDateBoxPaint.setShadowLayer(SHADOW_RADIUS, 0, 0, mWatchHandShadowColor);
            }
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            /* Dim display in mute mode. */
            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mHourPaint.setAlpha(inMuteMode ? 80 : 255);
                mMinutePaint.setAlpha(inMuteMode ? 80 : 255);
                mSecondPaint.setAlpha(inMuteMode ? 50 : 255);
                invalidate();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            /*
             * Calculate lengths of different hands based on watch screen size.
             */
            mSecondHandLength = (mCenterX * 0.93f);
            sMinuteHandLength = (mCenterX * 0.8f);
            sHourHandLength = (mCenterX * 0.6f);

            mTextWidth = (width * 0.68f);


            /* Scale loaded background image (more efficient) if surface dimensions change. */
            float scale = ((float) width) / (float) mBackgroundBitmap.getWidth();

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * scale),
                    (int) (mBackgroundBitmap.getHeight() * scale), true);

            /*
             * Create a gray version of the image only if it will look nice on the device in
             * ambient mode. That means we don't want devices that support burn-in
             * protection (slight movements in pixels, not great for images going all the way to
             * edges) and low ambient mode (degrades image quality).
             *
             * Also, if your watch face will know about all images ahead of time (users aren't
             * selecting their own photos for the watch face), it will be more
             * efficient to create a black/white version (png, etc.) and load that when you need it.
             */
            // For most Wear devices, width and height are the same, so we just chose one (width).
            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect topBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (horizontalOffset + sizeOfComplication),
                            ((sizeOfComplication / 2)-20),
                            (midpointOfScreen + horizontalOffset),
                            ((verticalOffset)-20));

            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
            topComplicationDrawable.setBounds(topBounds);

            Rect leftBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (horizontalOffset-20),
                            verticalOffset,
                            ((horizontalOffset + sizeOfComplication)-20),
                            (verticalOffset + sizeOfComplication));

            ComplicationDrawable leftComplicationDrawable =
                    mComplicationDrawableSparseArray.get(LEFT_COMPLICATION_ID);
            leftComplicationDrawable.setBounds(leftBounds);

            Rect bottomBounds =
                    // Left, Top, Right, Bottom
                    new Rect(
                            (horizontalOffset + sizeOfComplication),
                            ((verticalOffset + sizeOfComplication)+20),
                            (midpointOfScreen + horizontalOffset),
                            ((verticalOffset + (sizeOfComplication *2))+20));

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);

            Rect screenForBackgroundBound =
                    // Left, Top, Right, Bottom
                    new Rect(0, 0, width, height);

            ComplicationDrawable backgroundComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);

            if (!mBurnInProtection && !mLowBitAmbient) {
                initGrayBackgroundBitmap();
            }
        }

        private void initGrayBackgroundBitmap() {
            mGrayBackgroundBitmap = Bitmap.createBitmap(
                    mBackgroundBitmap.getWidth(),
                    mBackgroundBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mGrayBackgroundBitmap);
            Paint grayPaint = new Paint();
            ColorMatrix colorMatrix = new ColorMatrix();
            colorMatrix.setSaturation(0);  //converts to Grayscale
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
            grayPaint.setColorFilter(filter);
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, grayPaint);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TAP:

                    // If your background complication is the first item in your array, you need
                    // to walk backward through the array to make sure the tap isn't for a
                    // complication above the background complication.
                    for (int i = COMPLICATION_IDS.length - 1; i >= 0; i--) {
                        int complicationId = COMPLICATION_IDS[i];
                        ComplicationDrawable complicationDrawable =
                                mComplicationDrawableSparseArray.get(complicationId);

                        boolean successfulTap = complicationDrawable.onTap(x, y);
                        if (successfulTap) {
                            return;
                        }
                    }
                    break;
            }
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawComplications(canvas, now);
            drawWatchTicks(canvas);
            drawDate(canvas);
            drawWatchFace(canvas);
        }

        private void drawBackground(Canvas canvas) {

            if (mAmbient && (mLowBitAmbient || mBurnInProtection)) {
                canvas.drawColor(Color.BLACK);
            } else if (mAmbient) {
//                canvas.drawBitmap(mGrayBackgroundBitmap, 0, 0, mBackgroundPaint);
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);
            }
        }

        private void drawComplications(Canvas canvas, long currentTimeMillis) {
            int complicationId;
            ComplicationDrawable complicationDrawable;
            if (!mAmbient) {
                for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                    complicationId = COMPLICATION_IDS[i];
                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);
                    complicationDrawable.draw(canvas, currentTimeMillis);
                }
            }
        }

        private void drawDate (Canvas canvas) {
            /* Add Day/Date at the 3 location */
            SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss", Locale.UK);
            String strTime = sdf2.format(mCalendar.getTimeInMillis());

            SimpleDateFormat sdfDay = new SimpleDateFormat("EEE", Locale.UK);
            String strDay = sdfDay.format(mCalendar.getTimeInMillis());

            int yPos = (int) ((canvas.getHeight() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ;

            SimpleDateFormat sdfDay2 = new SimpleDateFormat("EEE", Locale.UK);
            String strDay2 = sdfDay2.format(mCalendar.getTimeInMillis());
            SimpleDateFormat sdfDate2 = new SimpleDateFormat("dd", Locale.UK);
            String strDate2 = sdfDate2.format(mCalendar.getTimeInMillis());
            String uSdfDay2 = strDay2.toUpperCase();

            Rect boundsa3 = new Rect();
            Rect boundsa3a = new Rect();
            Rect boundsa3b = new Rect();
            Rect boundsa3c = new Rect();

            final String mDayDate = uSdfDay2 + " " + strDate2;

            mTextPaint.getTextBounds(mDayDate,0,mDayDate.length(),boundsa3);
            mTextPaint.getTextBounds(uSdfDay2,0,uSdfDay2.length(),boundsa3a);
            mTextPaint.getTextBounds(strDate2,0,strDate2.length(),boundsa3b);
            mTextPaint.getTextBounds(" ",0," ".length(),boundsa3c);

            int dateBoxL = (int)((mTextWidth + (boundsa3.width()))-(boundsa3b.width()+4));
            int dateBoxR = (int) (mTextWidth + (boundsa3.width() + 4));


            if (!mAmbient) {
                canvas.drawRect(mTextWidth - 4,
                        (yPos - (boundsa3.height() + 4)),
                        (mTextWidth + (boundsa3a.width() + 6)),
                        yPos + 4,
                        mDateBoxPaint);
                canvas.drawRect(dateBoxL,
                        (yPos - (boundsa3.height() + 4)),
                        (mTextWidth + (boundsa3.width() + 4)),
                        yPos + 4,
                        mDateBoxPaint);
                canvas.drawText(mDayDate,
                        mTextWidth,
                        yPos,
                        mTextPaint);
            }
        }

        private void drawWatchTicks(Canvas canvas){
            float innerTickRadius = mCenterX - 15;
            float outerTickRadius = mCenterX;
            float minuteInnerTickRadius = mCenterX - 10;
            float minuteOuterTickRadius = mCenterX;
            float secondInnerTickRadius = mCenterX - 5;
            float secondOuterTickRadius = mCenterX;
            float innerTextRadius = mCenterX - 40;

            /* Draw ticks.
            * Hour Tick
            */
            if(!mAmbient){
                for (int textIndex = 1; textIndex < 2; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex),((mCenterX + innerX)+5), mCenterY + innerY,
                            mTextPaintHours);
                }
                for (int textIndex = 2; textIndex < 3; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex),((mCenterX + innerX)+7), mCenterY + innerY,
                            mTextPaintHours);
                }
                for (int textIndex = 4; textIndex < 5; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex),((mCenterX + innerX)+7), ((mCenterY + innerY)+15),
                            mTextPaintHours);
                }
                for (int textIndex = 5; textIndex < 6; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex),((mCenterX + innerX)+5), ((mCenterY + innerY)+15),
                            mTextPaintHours);
                }
                for (int textIndex = 7; textIndex < 8; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex),((mCenterX + innerX)-2), ((mCenterY + innerY)+15),
                            mTextPaintHours);
                }
                for (int textIndex = 8; textIndex < 9; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex), ((mCenterX + innerX) - 10), ((mCenterY + innerY)+15),
                            mTextPaintHours);
                }
                for (int textIndex = 10; textIndex < 11; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex),((mCenterX + innerX) -7), ((mCenterY + innerY)+5),
                            mTextPaintHours);
                }
                for (int textIndex = 11; textIndex < 12; textIndex++) {
                    float tickRot = (float) (textIndex * Math.PI * 2 / 12);
                    float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                    canvas.drawText(String.valueOf(textIndex),((mCenterX + innerX)-5), mCenterY + innerY,
                            mTextPaintHours);
                }
            } else {

                float tickRot = (float) (0 * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTextRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTextRadius;
                canvas.drawText("12", ((mCenterX + innerX) -15), mCenterY + innerY, mTextPaint);
                float tickRot3 = (float) (3 * Math.PI * 2 / 12);
                float innerX3 = (float) Math.sin(tickRot3) * innerTextRadius;
                float innerY3 = (float) -Math.cos(tickRot3) * innerTextRadius;
                canvas.drawText("3", ((mCenterX + innerX3)+5), ((mCenterY + innerY3)+12), mTextPaint);
                float tickRot6 = (float) (6 * Math.PI * 2 / 12);
                float innerX6 = (float) Math.sin(tickRot6) * innerTextRadius;
                float innerY6 = (float) -Math.cos(tickRot6) * innerTextRadius;
                canvas.drawText("6", ((mCenterX + innerX6) -8), ((mCenterY + innerY6) +20), mTextPaint);
                float tickRot9 = (float) (9 * Math.PI * 2 / 12);
                float innerX9 = (float) Math.sin(tickRot9) * innerTextRadius;
                float innerY9 = (float) -Math.cos(tickRot9) * innerTextRadius;
                canvas.drawText("9", ((mCenterX + innerX9) -20), ((mCenterY + innerY9)+10), mTextPaint);

            }

            for (int tickIndex = 0; tickIndex < 12; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 2 / 12);
                float innerX = (float) Math.sin(tickRot) * innerTickRadius;
                float innerY = (float) -Math.cos(tickRot) * innerTickRadius;
                float outerX = (float) Math.sin(tickRot) * outerTickRadius;
                float outerY = (float) -Math.cos(tickRot) * outerTickRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mHourTickPaint);
            }

            if(!mAmbient) {
                /* Minute Tick */
                for (int smallTickIndex = 0; smallTickIndex < 60; smallTickIndex++) {
                    float tickRot = (float) (smallTickIndex * Math.PI * 2 / 60);
                    float innerX = (float) Math.sin(tickRot) * minuteInnerTickRadius;
                    float innerY = (float) -Math.cos(tickRot) * minuteInnerTickRadius;
                    float outerX = (float) Math.sin(tickRot) * minuteOuterTickRadius;
                    float outerY = (float) -Math.cos(tickRot) * minuteOuterTickRadius;
                    canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                            mCenterX + outerX, mCenterY + outerY, mMinuteTickPaint);
                }
                for (int tinyTickIndex = 0; tinyTickIndex < 360; tinyTickIndex++) {
                    float tickRot = (float) (tinyTickIndex * Math.PI * 2 / 360);
                    float innerX = (float) Math.sin(tickRot) * secondInnerTickRadius;
                    float innerY = (float) -Math.cos(tickRot) * secondInnerTickRadius;
                    float outerX = (float) Math.sin(tickRot) * secondOuterTickRadius;
                    float outerY = (float) -Math.cos(tickRot) * secondOuterTickRadius;
                    canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                            mCenterX + outerX, mCenterY + outerY, mMinuteTickPaint);
                }
            }
                /* Inner White Circle */
            canvas.drawCircle(
                    mCenterX,
                    mCenterY,
                    HOUR_CIRCLE_STROKE_WIDTH,
                    mInnerCirclePaint);
            if(!mAmbient) {
                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        SECOND_CIRCLE_STROKE_WIDTH,
                        mInnerRedCirclePaint);
                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        mCenterX-1,
                        mOuterCirclePaint);
                canvas.drawCircle(
                        mCenterX,
                        mCenterY,
                        mCenterX-10,
                        mOuterCirclePaint);
            }

        }
        private void drawWatchFace(Canvas canvas) {
            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */
            final float seconds =
                    (mCalendar.get(Calendar.SECOND) + mCalendar.get(Calendar.MILLISECOND) / 1000f);
            final float secondsRotation = seconds * 6f;

            final float minutesRotation = mCalendar.get(Calendar.MINUTE) * 6f;

            final float hourHandOffset = mCalendar.get(Calendar.MINUTE) / 2f;
            final float hoursRotation = (mCalendar.get(Calendar.HOUR) * 30) + hourHandOffset;

            /*
             * Save the canvas state before we can begin to rotate it.
             */
            canvas.save();

            canvas.rotate(hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sHourHandLength,
                    mHourPaint);

            canvas.rotate(minutesRotation - hoursRotation, mCenterX, mCenterY);
            canvas.drawLine(
                    mCenterX,
                    mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                    mCenterX,
                    mCenterY - sMinuteHandLength,
                    mMinutePaint);

            /*
             * Ensure the "seconds" hand is drawn only when we are in interactive mode.
             * Otherwise, we only update the watch face once a minute.
             */
            if (!mAmbient) {
                canvas.rotate(secondsRotation - minutesRotation, mCenterX, mCenterY);
                canvas.drawLine(
                        mCenterX,
                        mCenterY - CENTER_GAP_AND_CIRCLE_RADIUS,
                        mCenterX,
                        mCenterY - mSecondHandLength,
                        mSecondPaint);

            }

            /* Restore the canvas' original orientation. */
            canvas.restore();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                // Preferences might have changed since last time watch face was visible.
                loadSavedPreferences();

                // With the rest of the watch face, we update the paint colors based on
                // ambient/active mode callbacks, but because the ComplicationDrawable handles
                // the active/ambient colors, we only need to update the complications' colors when
                // the user actually makes a change to the highlight color, not when the watch goes
                // in and out of ambient mode.
                setComplicationsActiveAndAmbientColors(mWatchHourMinuteColor);
                updateWatchHandStyle();

                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SimpleAnalogueWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SimpleAnalogueWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}