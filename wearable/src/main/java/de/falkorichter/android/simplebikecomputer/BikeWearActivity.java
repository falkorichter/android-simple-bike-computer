package de.falkorichter.android.simplebikecomputer;

import android.app.Activity;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.wearable.view.DismissOverlayView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.falkorichter.android.bluetooth.HeartRateConnector;
import de.falkorichter.android.bluetooth.NotifyConnector;
import de.falkorichter.android.bluetooth.SpeedAndCadenceConnector;


public class BikeWearActivity extends Activity implements HeartRateConnector.HeartRateListener, SpeedAndCadenceConnector.SpeedAndCadenceConnectorListener {
    private static final String TAG = BikeWearActivity.class.getSimpleName();

    private GestureDetectorCompat mGestureDetector;

    @InjectView(R.id.dismiss_overlay)
    DismissOverlayView mDismissOverlayView;

    @InjectView(R.id.heartRateButton)
    Button heartBeatButton;

    @InjectView(R.id.speedAndCadenceButton)
    Button speedAndCadenceButton;

    private int lastSpeed;
    private int lastTotalDistance;
    private int lastHeartbeatRSSI = 0;
    private HeartRateConnector heartRateConnector;
    private SpeedAndCadenceConnector speedAndCadenceConnector;
    private int heartRateMeasurementValue;
    private PowerManager.WakeLock wakeLock;
    private int lastBikeRSSI = 0;


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
        speedAndCadenceConnector = new SpeedAndCadenceConnector(manager.getAdapter(), this);
        speedAndCadenceConnector.setListener(this);
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


    @OnClick(R.id.finishActivityButton)
    public void onFinishActivity(View view) {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onPause() {
        if(wakeLock != null) {
            wakeLock.release();
        }
        heartRateConnector.disconnect();
        speedAndCadenceConnector.disconnect();
        super.onPause();
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
    protected void keepAwakeButtonTapped(Button keepAwakeButton) {
        if (this.wakeLock == null) {
            final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            this.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
            this.wakeLock.acquire();
            keepAwakeButton.setText("Save Battery");
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            this.wakeLock.release();
            this.wakeLock = null;
            keepAwakeButton.setText("Stay Awake");
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }

    @OnClick(R.id.heartRateButton)
    void heartRateButtonClicked() {
        if (heartRateConnector.isConnecting() || heartRateConnector.isConnected()) {
            heartRateConnector.disconnect();
        } else {
            heartRateConnector.scanAndAutoConnect();
        }
    }
    @OnClick(R.id.speedAndCadenceButton)
    void speedAndCadenceConnectorTapped(){
        if (speedAndCadenceConnector.isConnecting() || speedAndCadenceConnector.isConnected()) {
            speedAndCadenceConnector.disconnect();
        } else {
            speedAndCadenceConnector.scanAndAutoConnect();
        }
    }

    @Override
    public void heartRateChanged(int heartRateMeasurementValue) {
        this.heartRateMeasurementValue = heartRateMeasurementValue;
        updateHeartbeatButton();
    }


    @Override
    public void onRSSIUpdate(NotifyConnector connector, final int rssi) {
        if(connector == heartRateConnector) {
            this.lastHeartbeatRSSI = rssi;
            updateHeartbeatButton();
        } else if (connector == speedAndCadenceConnector){
            this.lastBikeRSSI = rssi;
            updateSpeedButton();
        }
    }

    private void updateHeartbeatButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                heartBeatButton.setText("heart beat: " + heartRateMeasurementValue + " bpm (" + lastHeartbeatRSSI + "db)");
            }
        });
    }

    @Override
    public void speedChanged(double speedInKilometersPerHour) {
        lastSpeed = (int) speedInKilometersPerHour;
        updateSpeedButton();
    }

    private void updateSpeedButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speedAndCadenceButton.setText(lastSpeed + "km/h (" + lastTotalDistance + "km total, " + lastBikeRSSI + "db)");
            }
        });
    }

    @Override
    public void onTotalDistanceChanged(double totalDistanceInMeters) {
        lastTotalDistance = (int) (totalDistanceInMeters / 1000);
        updateSpeedButton();
    }

    @Override
    public void onConnectionStateChanged(final NotifyConnector connector, final NotifyConnector.ConnectionState connectionState) {
        final int color = colorForState(connectionState);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Button button = null;
                if (connector == heartRateConnector) {
                    button = heartBeatButton;
                if (connectionState == NotifyConnector.ConnectionState.disconnected) {
                        lastBikeRSSI = 0;
                        Toast.makeText(BikeWearActivity.this, "disconnected heart rate", Toast.LENGTH_SHORT).show();
                        updateHeartbeatButton();
                    }
                } else if (connector == speedAndCadenceConnector) {
                    button = speedAndCadenceButton;
                    if(connectionState == NotifyConnector.ConnectionState.disconnected) {
                        lastBikeRSSI = 0;
                        Toast.makeText(BikeWearActivity.this, "disconnected speed", Toast.LENGTH_SHORT).show();
                        updateSpeedButton();
                    }
                }
                if (button != null) button.setTextColor(color);
            }
        });
    }

    private int colorForState(NotifyConnector.ConnectionState connectionState) {
        switch (connectionState){
            case connecting:
                return Color.YELLOW;
            case disconnected:
                return Color.RED;
            case connected:
                return Color.WHITE;
            case scanning:
                return Color.argb(255,255, 165, 0);
            case connectingConfirmed:
                return Color.argb(255, 255, 215, 0);
            case disconnecting:
                Color.argb(255, 175, 0, 0);
            default:
                return Color.CYAN;
        }
    }
}