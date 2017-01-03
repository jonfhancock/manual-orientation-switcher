package com.jonfhancock.manualorientationsample;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OrientationChange";
    private ManualOrientationSwitcher manualOrientationSwitcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        manualOrientationSwitcher = new ManualOrientationSwitcher();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                manualOrientationSwitcher.toggleOrientation();

            }
        });

    }



    @Override
    protected void onPause() {
        super.onPause();
        manualOrientationSwitcher.detatch();
    }



    @Override
    protected void onResume() {
        super.onResume();

        manualOrientationSwitcher.attach(this);
    }



}
