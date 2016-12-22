package com.jonfhancock.manualorientationsample;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OrientationChange";
    private OrientationEventListener orientationEventListener;
    private int lastOrientationRequested;
    private boolean systemOrientationLocked;
    private ContentObserver systemOrientationLockObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            try {
                systemOrientationLocked = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION) == 0;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    requestPortraitMode();
                } else {
                    requestLandscapeMode();
                }

            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(systemOrientationLockObserver);
        orientationEventListener.disable();
    }



    @Override
    protected void onResume() {
        super.onResume();

        getContentResolver().registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), false, systemOrientationLockObserver);
        systemOrientationLockObserver.onChange(true);
        setupOrientationListener();

    }

    private void requestLandscapeMode() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        lastOrientationRequested = Configuration.ORIENTATION_LANDSCAPE;
    }

    private void requestPortraitMode() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        lastOrientationRequested = Configuration.ORIENTATION_PORTRAIT;
    }

    private void setupOrientationListener() {
        lastOrientationRequested = Configuration.ORIENTATION_UNDEFINED;
        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_UI) {
            public int lastRecordedValue;
            // This constant defines the number of degrees of rotation away from perfect portrait or landscape
            // that we will use to decide if we should unlock the orientation
            public static final int ORIENTATION_BREAKPOINT = 5;


            @Override
            public void onOrientationChanged(int o) {
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

                int orientation = getResources().getConfiguration().orientation;

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
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

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
}
