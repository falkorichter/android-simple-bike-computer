package de.falkorichter.android.simplebikecomputer;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.DismissOverlayView;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.falkorichter.android.bluetooth.HeartRateConnector;
import de.falkorichter.android.bluetooth.NotifyConnector;


public class BikeWearActivity extends Activity implements  HeartRateConnector.HeartRateListener {
    private static final String TAG = BikeWearActivity.class.getSimpleName();

    private GestureDetectorCompat mGestureDetector;

    @InjectView(R.id.dismiss_overlay)
    DismissOverlayView mDismissOverlayView;

    @InjectView(R.id.heartRateButton)
    Button heartBeatButton;
    private int lastRSSI = 0;
    private HeartRateConnector heartRateConnector;
    private int heartRateMeasurementValue;
    private PowerManager.WakeLock wakeLock;


    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.bike_activity);
        ButterKnife.inject(this);

        mDismissOverlayView.setIntroText(R.string.intro_text);
        mDismissOverlayView.showIntroIfNecessary();
        mGestureDetector = new GestureDetectorCompat(this, new LongPressListener());

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        heartRateConnector = new HeartRateConnector(manager.getAdapter(), this);
        heartRateConnector.setListener(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouchEvent(event) || super.dispatchTouchEvent(event);
    }

    private class LongPressListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent event) {
            mDismissOverlayView.show();
        }
    }



    /**
     * Handles the button press to finish this activity and take the user back to the Home.
     */
    @OnClick(R.id.finishActivityButton)
    public void onFinishActivity(View view) {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.wakeLock.release();
        heartRateConnector.disconnect();
    }
    private void scroll(final int scrollDirection) {
        final ScrollView scrollView = (ScrollView) findViewById(R.id.scroll);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(scrollDirection);
            }
        });
    }

    @OnClick(R.id.keep_awake_button)
    protected void keepAwakeButtonTapped(Button keepAwakeButton){
        if(this.wakeLock == null){
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
            this.wakeLock.acquire();
            keepAwakeButton.setText("Save Battery");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }else{
            this.wakeLock.release();
            this.wakeLock = null;
            keepAwakeButton.setText("Stay Awake");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }

    @OnClick(R.id.heartRateButton)
    void heartRateButtonClicked(){
        if (heartRateConnector.isConnecting() || heartRateConnector.isConnected()){
            heartRateConnector.disconnect();
        } else {
            heartRateConnector.scanAndAutoConnect();
        }

    }

    @Override
    public void heartRateChanged(int heartRateMeasurementValue) {
        this.heartRateMeasurementValue = heartRateMeasurementValue;
        Log.d(TAG, "measured :" + heartRateMeasurementValue + "bpm");
        updateDisplay();
    }


    @Override
    public void onRSSIUpdate(NotifyConnector connector, final int rssi) {
        this.lastRSSI = rssi;
        updateDisplay();
    }

    private void updateDisplay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heartBeatButton.setText("heart beat: " + heartRateMeasurementValue + " bpm (" + lastRSSI + "db)");
            }
        });
    }

    @Override
    public void onHeartRateConnected(NotifyConnector connector, final boolean connected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(connected) {
                    heartBeatButton.setTextColor(Color.WHITE);
                } else {
                    heartBeatButton.setTextColor(Color.RED);
                }
            }
        });
    }
}