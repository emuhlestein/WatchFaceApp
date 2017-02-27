package com.intelliviz.wearapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Class for drawing watch face.
 * @author Ed Muhlestein
 */
public class WatchFaceAppService extends CanvasWatchFaceService {
    public static final String TAG = WatchFaceAppService.class.getSimpleName();
    private final SimpleDateFormat sDateFormat = new SimpleDateFormat("E, MMM d yyyy");
    public static final String LOCAL_ACTION2 = "weatherBroadCast2";
    public static final String WEATHER_MINTEMP_STR = "mintempstring";
    public static final String WEATHER_MAXTEMP_STR = "mmatempstring";
    public static final String WEATHER_IMAGE = "weatheriamge";

    /**
     * Default constructor.
     */
    public WatchFaceAppService() {
    }

    /**
     * Create the drawing enging.
     * @return The drawing engine.
     */
    public CanvasWatchFaceService.Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        private final String TAG = Engine.class.getSimpleName();
        private static final long COLON_UPDATE_RATE_MS = 500;
        private static final int LINE_YOFFSET = 40;
        private static final int LINE_LENGTH = 80;
        private static final int TEMP_YOFFSET = 40;
        static final int MSG_UPDATE_TIME = 0;
        private Calendar mCalendar;
        private Date mDate;
        private Bitmap mBackgroundBitmap = null;
        private Bitmap mScaledBackgroundBitmap = null;
        private String mMinTempString = "0";
        private String mMaxTempString = "99";
        private Bitmap mWeatherBitmap = null;
        private Paint mTimePaint;
        private Paint mDatePaint;
        private Paint mTempPaint;
        private float mTimeXOffset;
        private float mTimeYOffset;
        private float mDateXOffset;
        private float mDateYOffset;
        private float mColonWidth;
        private float mHourWidth;

        private boolean mDrawColon;

        long mColonUpdateRate = COLON_UPDATE_RATE_MS;

        // handler to update the time once a second in interactive mode
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long currentTimeMS = System.currentTimeMillis();
                            long delayMs = mColonUpdateRate - (currentTimeMS % mColonUpdateRate);
                            mUpdateTimeHandler
                                    .sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        /**
         * Receive changes to weather data: min temp, max temp, and image id.
         */
        private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get extra data included in the Intent
                mMinTempString = intent.getStringExtra(WEATHER_MINTEMP_STR);
                mMaxTempString = intent.getStringExtra(WEATHER_MAXTEMP_STR);
                byte[] bitmapData = intent.getByteArrayExtra(WEATHER_IMAGE);

                if(bitmapData != null && bitmapData.length > 0) {
                    mWeatherBitmap = BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
                    int width = mWeatherBitmap.getWidth() / 2;
                    int height = mWeatherBitmap.getHeight() / 2;
                    mWeatherBitmap = Bitmap.createScaledBitmap(mWeatherBitmap, width, height, true);
                }
                invalidate();
            }
        };

        /**
         * Listen for timezone changes.
         */
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            // load the background image
            Resources resources = WatchFaceAppService.this.getResources();
            Drawable backgroundDrawable = resources.getDrawable(R.drawable.sunrise, null);
            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceAppService.this)
            .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
            .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
            .setShowSystemUiTime(false)
            .build());

            mTimePaint = new Paint();
            mTimePaint.setTextSize(80);
            mTimePaint.setColor(Color.WHITE);
            mTimePaint.setAntiAlias(true);

            mTempPaint = new Paint();
            mTempPaint.setTextSize(40);
            mTempPaint.setColor(Color.WHITE);
            mTempPaint.setAntiAlias(true);

            mDatePaint = new Paint();
            mDatePaint.setTextSize(30);
            mDatePaint.setColor(Color.WHITE);
            mDatePaint.setAntiAlias(true);

            // In order to make text in the center, we need adjust its position
            mTimeXOffset = mTimePaint.measureText("12:00") / 2;
            mTimeYOffset = (mTimePaint.ascent() + mTimePaint.descent()) / 2;
            mDateXOffset = mDatePaint.measureText("XXX, XXX 00 0000") / 2;
            mDateYOffset = (mDatePaint.ascent() + mDatePaint.descent()) / 2;

            mColonWidth = mTimePaint.measureText(":");
            mHourWidth = mTimePaint.measureText("0");

            mCalendar = Calendar.getInstance();
            mDate = new Date();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);
            mDate.setTime(now);
            canvas.drawRect(0, 0, bounds.width(), bounds.height(), mTimePaint);
            canvas.drawColor(Color.GREEN);

            // draw background image
            canvas.drawBitmap(mScaledBackgroundBitmap, 0, 0, null);

            mDrawColon = (System.currentTimeMillis() % 1000) < 500;

            int hour = mCalendar.get(Calendar.HOUR);
            if(hour == 0) {
                hour = 12;
            }
            String hourString = String.valueOf(hour);

            float x = bounds.centerX() - mTimeXOffset;

            if(hour < 10) {
                x += mHourWidth;
            }

            // draw hour string
            canvas.drawText(hourString,
                    x,
                    bounds.centerY() + mTimeYOffset,
                    mTimePaint);

            x += mTimePaint.measureText(hourString);

            // draw blinking colon
            if (isInAmbientMode() || mDrawColon) {
                canvas.drawText(":", x, bounds.centerY() + mTimeYOffset, mTimePaint);
            }

            x += mColonWidth;

            int minute = mCalendar.get(Calendar.MINUTE);
            String minuteString = String.format("%02d", minute);

            // draw minute string
            canvas.drawText(minuteString,
                    x,
                    bounds.centerY() + mTimeYOffset,
                    mTimePaint);

            canvas.drawText(sDateFormat.format(new Date()),
                    bounds.centerX() - mDateXOffset + 10,
                    bounds.centerY() - mDateYOffset,
                    mDatePaint);

            int half_length = LINE_LENGTH / 2;
            int startX = bounds.centerX() - half_length;
            int startY = bounds.centerY() + LINE_YOFFSET;
            int stopX = bounds.centerX() + half_length;
            int stopY = bounds.centerY() + LINE_YOFFSET;
            canvas.drawLine(startX, startY, stopX, stopY, mDatePaint);

            x =  bounds.centerX()-20;
            int y = startY + TEMP_YOFFSET;
            String temp = mMaxTempString;

            // draw max temp
            canvas.drawText(temp, x, y, mTempPaint);

            x =  bounds.centerX() + 40;
            temp = mMinTempString;

            // draw min temp
            canvas.drawText(temp, x, y, mTempPaint);

            x = bounds.centerX() - 80;
            y = startY;

            // draw weather bitmap
            if(mWeatherBitmap != null) {
                canvas.drawBitmap(mWeatherBitmap, x, y, mTimePaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if(visible) {
                registerReceivers();
                mCalendar.setTimeZone(TimeZone.getDefault());
            } else {
                unregisterReceivers();
            }
            updateTimer();
        }

        @Override
        public void onSurfaceChanged(
                SurfaceHolder holder, int format, int width, int height) {
            if (mBackgroundBitmap == null
                    || mBackgroundBitmap.getWidth() != width
                    || mBackgroundBitmap.getHeight() != height) {
                mScaledBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                        width, height, true /* filter */);
            }
            super.onSurfaceChanged(holder, format, width, height);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private void registerReceivers() {
            registerWatchDataReceiver();
            registerTimeZoneReceiver();
        }

        private void unregisterReceivers() {
            unregisterWatchDataReceiver();
            unregisterTimeZoneReceiver();
        }

        private void registerWatchDataReceiver() {
            IntentFilter filter = new IntentFilter(LOCAL_ACTION2);
            LocalBroadcastManager.getInstance(WatchFaceAppService.this).registerReceiver(mMessageReceiver, filter);
        }

        private void unregisterWatchDataReceiver() {
            LocalBroadcastManager.getInstance(WatchFaceAppService.this).unregisterReceiver(mMessageReceiver);
        }

        private void registerTimeZoneReceiver() {
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_LOCALE_CHANGED);
            WatchFaceAppService.this.registerReceiver(mReceiver, filter);
        }

        private void unregisterTimeZoneReceiver() {
            WatchFaceAppService.this.unregisterReceiver(mReceiver);
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}
