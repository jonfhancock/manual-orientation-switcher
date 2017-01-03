package com.jonfhancock.manualorientationsample;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.SensorManager;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.OrientationEventListener;

/**
 * Created by jonhancock on 1/3/17.
 */

public class ManualOrientationSwitcher {
    private static final String TAG = "ManualOrientation";
    private OrientationEventListener orientationEventListener;
    private int lastOrientationRequested;
    private boolean systemOrientationLocked;
    private Activity activity;
    private ContentObserver systemOrientationLockObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if(activity == null || activity.isFinishing()){
                return;
            }
            try {

                systemOrientationLocked = Settings.System.getInt(activity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 0;
                Log.d(TAG, "System orientation locked = " + systemOrientationLocked);

            } catch (Settings.SettingNotFoundException e) {
                // Do Nothing.  If this device cannot lock orientation, we don't need special treatment for it.
            }
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }
    };


    public void attach(Activity activity){
        this.activity = activity;
        activity.getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, systemOrientationLockObserver);
        systemOrientationLockObserver.onChange(true);
        setupOrientationListener();
    }
    public void detatch(){
        if(activity != null) {
            activity.getContentResolver().unregisterContentObserver(systemOrientationLockObserver);
        }
        orientationEventListener.disable();
        this.activity = null;
    }
    private void setupOrientationListener() {
        if(activity == null || activity.isFinishing()){
            return;
        }
        lastOrientationRequested = Configuration.ORIENTATION_UNDEFINED;
        orientationEventListener = new OrientationEventListener(activity, SensorManager.SENSOR_DELAY_UI) {
            public int lastRecordedValue;
            // This constant defines the number of degrees of rotation away from perfect portrait or landscape
            // that we will use to decide if we should unlock the orientation
            public static final int ORIENTATION_BREAKPOINT = 5;


            @Override
            public void onOrientationChanged(int o) {
                if(activity == null || activity.isFinishing()){
                    this.disable();
                    return;
                }
                // Ignore anything that happens when the device is laying on its back or face.
                if (o == -1) {
                    return;
                }
                // Ignore drastic changes. Sometimes we get an erroneous value from the sensor.
                if (Math.abs(o - lastRecordedValue) > 30) {
                    lastRecordedValue = o;
                    return;
                }
                lastRecordedValue = o;

                int orientation = activity.getResources().getConfiguration().orientation;

                boolean inRequestedOrientation = lastOrientationRequested == orientation && !systemOrientationLocked;
                boolean shouldUnlockForLandscape = orientation == Configuration.ORIENTATION_LANDSCAPE && (Math.abs(o - 270) <= ORIENTATION_BREAKPOINT || Math.abs(o - 90) <= ORIENTATION_BREAKPOINT);
                boolean shouldUnlockForPortrait = orientation == Configuration.ORIENTATION_PORTRAIT && (o <= ORIENTATION_BREAKPOINT || 360 - o <= ORIENTATION_BREAKPOINT);
                if (inRequestedOrientation && (shouldUnlockForLandscape || shouldUnlockForPortrait)) {
                    lastOrientationRequested = Configuration.ORIENTATION_UNDEFINED;
                    // Delay unlocking the orientation because if we do it right away,
                    // the app will switch temporarily to the opposite orientation first, then unlock
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(activity == null || activity.isFinishing()){
                                return;
                            }
                            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

                        }
                    }, 1000);
                }

            }
        };
        if (orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable();
            Log.d(TAG, "Listener can detect orientation");

        } else {
            orientationEventListener.disable();
            Log.d(TAG, "Listener cannot detect orientation");

        }
    }
    private void requestLandscapeMode() {
        if(activity == null || activity.isFinishing()){
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        lastOrientationRequested = Configuration.ORIENTATION_LANDSCAPE;
    }

    private void requestPortraitMode() {
        if(activity == null || activity.isFinishing()){
            return;
        }
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        lastOrientationRequested = Configuration.ORIENTATION_PORTRAIT;
    }
    public void toggleOrientation() {
        if(activity == null || activity.isFinishing()){
            return;
        }
        if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
            lastOrientationRequested = Configuration.ORIENTATION_PORTRAIT;
        } else {
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
            lastOrientationRequested = Configuration.ORIENTATION_LANDSCAPE;
        }
    }
}
