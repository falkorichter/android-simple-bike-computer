package de.falkorichter.android.simplebikecomputer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.falkorichter.android.simplebikecomputer.bluetooth.BluetoothUtil;
import de.falkorichter.android.simplebikecomputer.bluetooth.HeartRateConnector;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class Connect extends Activity implements HeartRateConnector.HeartRateListener {

    private static final UUID CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID CSC_CHARACTERISTIC_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");
    private static final UUID BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final String TAG = Connect.class.getSimpleName();
    public static final int NOT_SET = Integer.MIN_VALUE;

    private BluetoothManager bluetooth;
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            connectingToGatt = false;
            super.onConnectionStateChange(gatt, status, state);
            Log.d(TAG, "onConnectionStateChange gatt:" + BluetoothUtil.gattToString(gatt) + " status:" + BluetoothUtil.statusToString(status) + " state:" + BluetoothUtil.connectionStateToString(state));

            updateConnectionStateDisplay(state);

            switch (state) {
                case BluetoothGatt.STATE_CONNECTED: {
                    showText("STATE_CONNECTED", Style.INFO);
                    setConnectedGatt(gatt);
                    gatt.discoverServices();
                    break;
                }
                case BluetoothGatt.STATE_DISCONNECTED:
                    showText("STATE_DISCONNECTED", Style.ALERT);
                case BluetoothGatt.GATT_FAILURE: {
                    setConnectedGatt(null);
                    break;
                }
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            updateRssiDisplay(rssi);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered status:" + BluetoothUtil.statusToString(status));
            BluetoothGattCharacteristic valueCharacteristic = gatt.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
            boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
            Log.d(TAG, "registered for updates " + (notificationSet ? "successfully" : "unsuccessfully") );

            BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
            Log.d(TAG, "wrote Descriptor for updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully") );
        }

        double lastWheelTime = NOT_SET;
        long lastWheelCount = NOT_SET;
        double wheelSize = 2.17;

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();

            final long cumulativeWheelRevolutions       = (value[1] & 0xff) | ((value[2] & 0xff) << 8) | ((value[3] & 0xff) << 16) | ((value[4] & 0xff) << 24);
            final int lastWheelEventReadValue           = (value[5] & 0xff) | ((value[6] & 0xff) << 8);
            final int cumulativeCrankRevolutions        = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
            final int lastCrankEventReadValue           = (value[9] & 0xff) | ((value[10] & 0xff) << 8);

            double lastWheelEventTime = lastWheelEventReadValue / 1024.0;

            Log.d(TAG, "onCharacteristicChanged " + cumulativeWheelRevolutions + ":" + lastWheelEventReadValue + ":" + cumulativeCrankRevolutions + ":" + lastCrankEventReadValue);

            if (lastWheelTime == NOT_SET){
                lastWheelTime = lastWheelEventTime;
            }
            if (lastWheelCount == NOT_SET){
                lastWheelCount = cumulativeWheelRevolutions;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    distanceLabel.setText( String.format( "Distance: %.2f", cumulativeWheelRevolutions * wheelSize));
                }
            });

            long numberOfWheelRevolutions = cumulativeWheelRevolutions - lastWheelCount;

            if (lastWheelTime  != lastWheelEventTime && numberOfWheelRevolutions > 0){
                double timeDiff = lastWheelEventTime - lastWheelTime;

                double speedinMetersPerSeconds = (wheelSize * numberOfWheelRevolutions) / timeDiff;
                double speedInKilometersPerHour = speedinMetersPerSeconds * 3.6;

                showSpeed((int) speedInKilometersPerHour);
                Log.d(TAG, "speed:" + speedInKilometersPerHour);

                lastWheelCount = cumulativeWheelRevolutions;
                lastWheelTime = lastWheelEventTime;
            }

            gatt.readRemoteRssi();
        }
    };
    private Crouton currentCrouton;
    private HeartRateConnector heartRateConnector;

    private void showSpeed(final int speedInKilometersPerHour) {
        final String text = getString(R.string.speed, speedInKilometersPerHour);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                speedTextView.setText(text);
            }
        });
    }

    private void showText(final String text, final Style style) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(currentCrouton != null){
                    currentCrouton.cancel();
                }
                currentCrouton = Crouton.makeText(Connect.this, text, style);
                currentCrouton.show();
            }
        });
    }

    private BluetoothGatt connectedGatt;

    @InjectView(R.id.disconnect_button)
    Button disconnectButton;

    @InjectView(R.id.connect_button)
    Button connectButton;

    @InjectView(R.id.distance_label)
    TextView distanceLabel;

    @InjectView(R.id.connection_state_textView)
    TextView connectionStateTextView;

    @InjectView(R.id.rssiTextView)
    TextView rssiTextView;

    @InjectView(R.id.auto_connect_checkbox)
    CheckBox autoConnectCheckBox;

    @InjectView(R.id.keep_awake_button)
    Button keepAwakeButton;

    @InjectView(R.id.speed_TextView)
    TextView speedTextView;

    private boolean connectingToGatt;

    private final Object connectingToGattMonitor = new Object();
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        bluetooth = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        ButterKnife.inject(this);

        disconnectButton.setEnabled(false);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.heartRateConnector.disconnect();
    }

    @OnClick(R.id.connect_heartrate_button)
    void connectToHeartRateSensor(){
        heartRateConnector = new HeartRateConnector(bluetooth.getAdapter(), this);
        heartRateConnector.setListener(this);
        heartRateConnector.scanAndAutoConnect();
    }

    @OnClick(R.id.connect_button)
    protected void connectButtonTapped() {
        final BluetoothAdapter adapter = bluetooth.getAdapter();
        UUID[] serviceUUIDs = new UUID[]{CSC_SERVICE_UUID};
        adapter.startLeScan(serviceUUIDs, new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d(TAG, "found device " + device.getAddress());
                synchronized (connectingToGattMonitor){
                    if (/*device.getName().contains("BSCBLE V1.5") && */!connectingToGatt) {
                        connectingToGatt = true;
                        Log.d(TAG, "connecting to " + device.getAddress());
                        device.connectGatt(Connect.this, autoConnectCheckBox.isChecked(), bluetoothGattCallback);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectButton.setEnabled(false);
                            }
                        });
                        updateRssiDisplay(rssi);

                        adapter.stopLeScan(this);
                    }
                }
            }
        });
        connectButton.setEnabled(false);
    }

    @OnClick(R.id.disconnect_button)
    protected void disconnectButtonTapped() {
        connectedGatt.disconnect();
        connectedGatt.close();
        setConnectedGatt(null);
    }

    @OnClick(R.id.keep_awake_button)
    protected void keepAwakeButtonTapped(){
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.connect, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setConnectedGatt(final BluetoothGatt connectedGatt) {
        this.connectedGatt = connectedGatt;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disconnectButton.setEnabled(connectedGatt != null);
                connectButton.setEnabled(connectedGatt == null);
            }
        });
    }

    void updateConnectionStateDisplay(int status){
        final String text = BluetoothUtil.connectionStateToString(status);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionStateTextView.setText(text);
            }
        });
    }

    void updateRssiDisplay(final int rssi){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                rssiTextView.setText(Connect.this.getString(R.string.rssi_format, rssi));
            }
        });
    }

    @InjectView(R.id.connect_heartrate_button)
    Button connectHeartrateButton;
    boolean heartRateConnected = false;
    int heartRateRSSI = 0;
    int heartRate = 0;

    @Override
    public void onRSSIUpdate(int rssi) {
        heartRateRSSI = rssi;
        updateHeartRateButton();
    }

    @Override
    public void heartRateChanged(int heartRateMeasurementValue) {
        heartRate = heartRateMeasurementValue;
        updateHeartRateButton();
    }

    @Override
    public void onHeartRateConnected(boolean b) {
        heartRateConnected = b;
        updateHeartRateButton();
    }

    private void updateHeartRateButton() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                builder.append(heartRateConnected ? "connected" : "disconnected");
                builder.append(" heard rate: ").append(heartRate).append("bpm");
                builder.append(" rssi:").append(heartRateRSSI);
                connectHeartrateButton.setText(builder.toString());
            }
        });
    }
}
