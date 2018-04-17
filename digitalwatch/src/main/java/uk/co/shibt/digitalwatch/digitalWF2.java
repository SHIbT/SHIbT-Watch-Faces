package uk.co.shibt.digitalwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.rendering.ComplicationDrawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class digitalWF2 extends CanvasWatchFaceService {
    private static final int BACKGROUND_COMPLICATION_ID = 0;

    private static final Typeface MONOTYPE = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
    private static final int TOP_COMPLICATION_ID = 100;
    private static final int BOTTOM_COMPLICATION_ID = 101;
    private static final int[] COMPLICATION_IDS = {
            BACKGROUND_COMPLICATION_ID,
            TOP_COMPLICATION_ID,
            BOTTOM_COMPLICATION_ID,
    };

    public static class EnglishNumberToWords {

        private static final String[] tensNames = { "", " ten", " twenty", " thirty", " forty",
                " fifty" };

        private static final String[] numNames = { "", " one", " two", " three", " four", " five",
                " six", " seven", " eight", " nine", " ten", " eleven", " twelve", " thirteen",
                " fourteen", " fifteen", " sixteen", " seventeen", " eighteen", " nineteen" };

        private static String convertLessThanOneThousand(int number)
        {
            String soFar;

            if (number % 100 < 20)
            {
                soFar = numNames[number % 100];
                number /= 100;
            } else
            {
                soFar = numNames[number % 10];
                number /= 10;

                soFar = tensNames[number % 10] + soFar;
                number /= 10;
            }
            if (number == 0)
                return soFar;
            return numNames[number] + " hundred" + soFar;
        }
        public static String convert(long number)
        {
            // 0 to 999 999 999 999
            if (number == 0)
            {
                return "zero";
            }

            String snumber = Long.toString(number);

            // pad with "0"
            String mask = "000000000000";
            DecimalFormat df = new DecimalFormat(mask);
            snumber = df.format(number);

            // XXXnnnnnnnnn
            int billions = Integer.parseInt(snumber.substring(0, 3));
            // nnnXXXnnnnnn
            int millions = Integer.parseInt(snumber.substring(3, 6));
            // nnnnnnXXXnnn
            int hundredThousands = Integer.parseInt(snumber.substring(6, 9));
            // nnnnnnnnnXXX
            int thousands = Integer.parseInt(snumber.substring(9, 12));

            String tradBillions;
            switch (billions)
            {
                case 0:
                    tradBillions = "";
                    break;
                case 1:
                    tradBillions = convertLessThanOneThousand(billions) + " billion ";
                    break;
                default:
                    tradBillions = convertLessThanOneThousand(billions) + " billion ";
            }
            String result = tradBillions;

            String tradMillions;
            switch (millions)
            {
                case 0:
                    tradMillions = "";
                    break;
                case 1:
                    tradMillions = convertLessThanOneThousand(millions) + " million ";
                    break;
                default:
                    tradMillions = convertLessThanOneThousand(millions) + " million ";
            }
            result = result + tradMillions;

            String tradHundredThousands;
            switch (hundredThousands)
            {
                case 0:
                    tradHundredThousands = "";
                    break;
                case 1:
                    tradHundredThousands = "one thousand ";
                    break;
                default:
                    tradHundredThousands = convertLessThanOneThousand(hundredThousands) + " thousand ";
            }
            result = result + tradHundredThousands;

            String tradThousand;
            tradThousand = convertLessThanOneThousand(thousands);
            result = result + tradThousand;

            // remove extra spaces!
            return result.replaceAll("^\\s+", "").replaceAll("\\b\\s{2,}\\b", " ");
        }

    }

    private static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {//0 Background
                    ComplicationData.TYPE_LARGE_IMAGE
            },
            { //1 Top
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_LARGE_IMAGE
            },
            { //3 Bottom
                    ComplicationData.TYPE_RANGED_VALUE,
                    ComplicationData.TYPE_ICON,
                    ComplicationData.TYPE_LONG_TEXT,
                    ComplicationData.TYPE_LARGE_IMAGE
            },
    };
    /**
     * Update rate in milliseconds for interactive mode. Defaults to one second
     * because the watch face needs to update seconds in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);
    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    private String TAG = "SHIbT_digitalWF2";

    public static int getComplicationId(
            ComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case BACKGROUND:
                return BACKGROUND_COMPLICATION_ID;
            case TOP:
                return TOP_COMPLICATION_ID;
            case BOTTOM:
                return BOTTOM_COMPLICATION_ID;
            default:
                return -1;
        }
    }

    public static int[] getComplicationIds() {
        return COMPLICATION_IDS;
    }

    public static int[] getSupportedComplicationTypes(
            ComplicationConfigRecyclerViewAdapter.ComplicationLocation complicationLocation) {
        switch (complicationLocation) {
            case BACKGROUND:
                return COMPLICATION_SUPPORTED_TYPES[0];
            case TOP:
                return COMPLICATION_SUPPORTED_TYPES[1];
            case BOTTOM:
                return COMPLICATION_SUPPORTED_TYPES[3];
            default:
                return new int[]{};
        }
    }

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<digitalWF2.Engine> mWeakReference;

        public EngineHandler(digitalWF2.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            digitalWF2.Engine engine = mWeakReference.get();
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

        private static final int SMALL_RADIUS = 3;
        private static final int BIG_RADIUS = 6;
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private final Rect textBounds = new Rect();
        public int level, mTextPaintColor, mBackgroundPaintColor;
        public float lvl, battCircle, sweepAngle, sweepAngleRev, mWidth, mCenterX, mCenterY, mHeight, mXOffset, mYOffset;
        SharedPreferences mSharedPref;

        private Calendar mCalendar;
        private boolean mRegisteredTimeZoneReceiver = false;
        private boolean mMuteMode, mLowBitAmbient, mBurnInProtection, mAmbient;
        private boolean mRegisteredBattReceiver = false;
        private Paint mbattPaint, mBackgroundPaint, mTextPaint, mTextPaintm, mTextPaints, mTextPaintxs, mBattVoid;
        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;
        private SparseArray<ComplicationDrawable> mComplicationDrawableSparseArray;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        int drkGrey = ContextCompat.getColor(getApplicationContext(), R.color.drkGrey);

        private final BroadcastReceiver mBattReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                lvl = level / 100f;
                battCircle = mCenterX * lvl;

                if (level > 66) {
                    String bl = String.format("%02x", (100 - level) * 255 / 33);
                    bl = "#" + bl + "FF00";
                    mbattPaint.setColor(Color.parseColor(bl));
                } else if (level > 16) {
                    String bl = String.format("%02x", (level - 16) * 255 / 50);
                    bl = "#FF" + bl + "00";
                    mbattPaint.setColor(Color.parseColor(bl));
                } else {
                    String bl = "#FF0000";
                    mbattPaint.setColor(Color.parseColor(bl));
                }

                sweepAngle = 170 - (170 * lvl);
                sweepAngleRev = (170 * lvl) - 170;
                invalidate();
            }
        };

        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Context context = getApplicationContext();
            mSharedPref =
                    context.getSharedPreferences(
                            getString(R.string.complication_preference_file_key),
                            Context.MODE_PRIVATE);

            setWatchFaceStyle(new WatchFaceStyle.Builder(digitalWF2.this)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();

            Resources resources = digitalWF2.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            // Initializes background.
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.background));

            // Initializes Watch Face.
            mTextPaint = new Paint();
            mTextPaint.setTypeface(MONOTYPE);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));

            mTextPaintm = new Paint();
            mTextPaintm.setTypeface(MONOTYPE);
            mTextPaintm.setAntiAlias(true);
            mTextPaintm.setTextAlign(Paint.Align.CENTER);
            mTextPaintm.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));

            mTextPaints = new Paint();
            mTextPaints.setTypeface(MONOTYPE);
            mTextPaints.setAntiAlias(true);
            mTextPaints.setTextAlign(Paint.Align.CENTER);
            mTextPaints.setColor(drkGrey);

            mTextPaintxs = new Paint();
            mTextPaintxs.setTypeface(MONOTYPE);
            mTextPaintxs.setAntiAlias(true);
            mTextPaintxs.setTextAlign(Paint.Align.CENTER);
            mTextPaintxs.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text)
            );

            mbattPaint = new Paint();
            mbattPaint.setStyle(Paint.Style.STROKE);
            mbattPaint.setAntiAlias(true);
            mbattPaint.setStrokeWidth(SMALL_RADIUS);

            mBattVoid = new Paint();
            mBattVoid.setAntiAlias(true);
            mBattVoid.setStyle(Paint.Style.STROKE);
            mBattVoid.setStrokeWidth(BIG_RADIUS);
            mBattVoid.setColor(Color.BLACK);

            mBackgroundPaintColor = getColor(R.color.background);
            mTextPaintColor = getColor(R.color.digital_text);

            loadSavedPreferences();
            initializeComplications();
        }
            private void loadSavedPreferences() {

                String backgroundColorResourceName =
                        getApplicationContext().getString(R.string.saved_background_color);

                mBackgroundPaintColor = mSharedPref.getInt(backgroundColorResourceName, Color.BLACK);
            }

            private void initializeComplications() {

                mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

                ComplicationDrawable topComplicationDrawable =
                        new ComplicationDrawable(getApplicationContext());

                ComplicationDrawable bottomComplicationDrawable =
                        new ComplicationDrawable(getApplicationContext());

                ComplicationDrawable leftComplicationDrawable =
                        new ComplicationDrawable(getApplicationContext());

                ComplicationDrawable rightComplicationDrawable =
                        new ComplicationDrawable(getApplicationContext());

                ComplicationDrawable backgroundComplicationDrawable =
                        new ComplicationDrawable(getApplicationContext());

                mComplicationDrawableSparseArray = new SparseArray<>(COMPLICATION_IDS.length);

                mComplicationDrawableSparseArray.put(
                        TOP_COMPLICATION_ID, topComplicationDrawable);
                mComplicationDrawableSparseArray.put(
                        BOTTOM_COMPLICATION_ID, bottomComplicationDrawable);
                mComplicationDrawableSparseArray.put(
                        BACKGROUND_COMPLICATION_ID, backgroundComplicationDrawable);

                setActiveComplications(COMPLICATION_IDS);
            }

            private void setComplicationsActiveAndAmbientColors(int primaryComplicationColor) {
                int complicationId;
                ComplicationDrawable complicationDrawable;

                for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                    complicationId = COMPLICATION_IDS[i];
                    complicationDrawable = mComplicationDrawableSparseArray.get(complicationId);

                    if (complicationId == BACKGROUND_COMPLICATION_ID) {
                        complicationDrawable.setBackgroundColorActive(Color.BLACK);
                    } else {
                        //complicationDrawable.setBorderColorActive(primaryComplicationColor);
                        complicationDrawable.setRangedValuePrimaryColorActive(primaryComplicationColor);
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
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            if (mRegisteredBattReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            IntentFilter bLevel = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            digitalWF2.this.registerReceiver(mTimeZoneReceiver, filter);
            digitalWF2.this.registerReceiver(mBattReceiver, bLevel);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            digitalWF2.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = digitalWF2.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);
            float textSizes = resources.getDimension(isRound
                    ? R.dimen.digital_min_text_size_round : R.dimen.digital_min_text_size);
            float textSizexs = resources.getDimension(isRound
                    ? R.dimen.digital_batt_text_size_round : R.dimen.digital_batt_text_size);

            mTextPaint.setTextSize(textSize);
            mTextPaintm.setTextSize(textSizes);
            mTextPaints.setTextSize(textSizes);
            mTextPaintxs.setTextSize(textSizexs);
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

            mActiveComplicationDataSparseArray.put(complicationId, complicationData);

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
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */
            mCenterX = width / 2f;
            mCenterY = height / 2f;
            mWidth = width;
            mHeight = height;

            int sizeOfComplication = width / 4;
            int midpointOfScreen = width / 2;

            int horizontalOffset = (midpointOfScreen - sizeOfComplication) / 2;
            int verticalOffset = midpointOfScreen - (sizeOfComplication / 2);

            Rect topBounds =
                    new Rect(
                            (horizontalOffset + 50),
                            ((sizeOfComplication / 2) - 20),
                            ((midpointOfScreen + horizontalOffset + sizeOfComplication) - 50),
                            ((verticalOffset) - 20));

            ComplicationDrawable topComplicationDrawable =
                    mComplicationDrawableSparseArray.get(TOP_COMPLICATION_ID);
            topComplicationDrawable.setBounds(topBounds);

            Rect bottomBounds =
                    new Rect(
                            (horizontalOffset + 50),
                            ((verticalOffset + sizeOfComplication) + 20),
                            ((midpointOfScreen + horizontalOffset + sizeOfComplication) - 50),
                            ((verticalOffset + (sizeOfComplication * 2)) + 20));

            ComplicationDrawable bottomComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BOTTOM_COMPLICATION_ID);
            bottomComplicationDrawable.setBounds(bottomBounds);

            Rect screenForBackgroundBound =
                    new Rect(0, 0, width, height);

            ComplicationDrawable backgroundComplicationDrawable =
                    mComplicationDrawableSparseArray.get(BACKGROUND_COMPLICATION_ID);
            backgroundComplicationDrawable.setBounds(screenForBackgroundBound);

        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;
            updateWatchHandStyle();

            ComplicationDrawable complicationDrawable;

            for (int i = 0; i < COMPLICATION_IDS.length; i++) {
                complicationDrawable = mComplicationDrawableSparseArray.get(COMPLICATION_IDS[i]);
                complicationDrawable.setInAmbientMode(mAmbient);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mTextPaint.setAntiAlias(false);
            }

        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);
            boolean inMuteMode = (interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE);

            if (mMuteMode != inMuteMode) {
                mMuteMode = inMuteMode;
                mTextPaint.setAlpha(inMuteMode ? 80 : 255);
                mTextPaintm.setAlpha(inMuteMode ? 80 : 255);
                mTextPaints.setAlpha(inMuteMode ? 80 : 255);
                invalidate();
            }
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TAP:
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
            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas);
            drawProgress(canvas);
            drawComplications(canvas, now);
            drawWatchFace(canvas);
        }

        private void drawBackground(Canvas canvas){


            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawColor(Color.BLACK);
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

        private void drawProgress(Canvas canvas) {
            if (!mAmbient) {
                RectF rectF = new RectF(0 + SMALL_RADIUS, 0 + SMALL_RADIUS,
                        mWidth - SMALL_RADIUS, mHeight - SMALL_RADIUS);
                canvas.drawArc(rectF, 280, 340, false, mbattPaint);
                canvas.drawArc(rectF, 280, sweepAngle, true, mBattVoid);
                canvas.drawText(String.valueOf(level), mCenterX, 20, mTextPaintxs);
                canvas.drawArc(rectF, 260, sweepAngleRev, true, mBattVoid);
            }
        }

        private void drawWatchFace(Canvas canvas) {
            int mTextHeight, mTextWidth;

            Paint p = new Paint();

            String Hour = String.format("%02d%02d", mCalendar.get(Calendar.HOUR_OF_DAY),
                    mCalendar.get(Calendar.MINUTE));
            String Second = String.format("%02d", mCalendar.get(Calendar.SECOND));
            int intSecond = Integer.parseInt(Second);

            String wordSeconds = EnglishNumberToWords.convertLessThanOneThousand(intSecond);

            mTextPaint.getTextBounds(Hour, 0, Hour.length(), textBounds);
            mTextHeight = textBounds.height(); // Use height from getTextBounds()
            mTextWidth = (int) p.measureText(Hour);

            if (!mAmbient) {
                canvas.drawText(Hour, mCenterX,
                        mCenterY + (mTextHeight / 2f), mTextPaint);
                canvas.drawText(wordSeconds, mCenterX - (mTextWidth / 2f),
                        mCenterY + (mTextHeight + 5), mTextPaints);
            } else {
                canvas.drawText(Hour, mCenterX, mCenterY + (mTextHeight / 2f), mTextPaint);
            }
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
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
