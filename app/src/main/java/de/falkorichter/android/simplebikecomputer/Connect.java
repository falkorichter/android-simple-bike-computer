package de.falkorichter.android.simplebikecomputer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import de.falkorichter.android.simplebikecomputer.utils.BluetoothUtil;


public class Connect extends Activity {

    private static final UUID CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID CSC_CHARACTERISTIC_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

    private static final UUID CSC_CHARACTERISTIC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");



    private static final String TAG = Connect.class.getSimpleName();

    private BluetoothManager bluetooth;
    private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            super.onConnectionStateChange(gatt, status, state);
            Log.d(TAG, "onConnectionStateChange gatt:" + BluetoothUtil.gattToString(gatt) + " status:" + BluetoothUtil.statusToString(status) + " state:" + BluetoothUtil.connectionStateToString(state));
            switch (state) {
                case BluetoothGatt.STATE_CONNECTED: {
                    setConnectedGatt(gatt);
                    gatt.discoverServices();
                    break;
                }
                case BluetoothGatt.STATE_DISCONNECTED:
                case BluetoothGatt.GATT_FAILURE: {
                    setConnectedGatt(null);
                    break;
                }
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            Log.d(TAG, "onServicesDiscovered status:" + BluetoothUtil.statusToString(status));
            BluetoothGattCharacteristic valueCharacteristic = gatt.getService(CSC_SERVICE_UUID).getCharacteristic(CSC_CHARACTERISTIC_UUID);
            boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);
            Log.d(TAG, "registered for updates " + (notificationSet ? "successfully" : "unsuccessfully") );

            BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(CSC_CHARACTERISTIC_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
            Log.d(TAG, "wrote Descriptor for updates " + (writeDescriptorSuccess ? "successfully" : "unsuccessfully") );
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] value = characteristic.getValue();

            final long cumulativeWheelRevolutions     = (value[1] & 0xff) | ((value[2] & 0xff) << 8) | ((value[3] & 0xff) << 16) | ((value[4] & 0xff) << 24);
            int lastWheelEventReadValue         = (value[5] & 0xff) | ((value[6] & 0xff) << 8);
            final int cumulativeCrankRevolutions      = (value[7] & 0xff) | ((value[8] & 0xff) << 8);
            int lastCrankEventReadValue         = (value[9] & 0xff) | ((value[10] & 0xff) << 8);

            Log.d(TAG, "onCharacteristicChanged " + cumulativeWheelRevolutions + ":" + lastWheelEventReadValue + ":" + cumulativeCrankRevolutions + ":" + lastCrankEventReadValue);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    distanceLabel.setText( String.format( "Distance: %.2f", cumulativeWheelRevolutions * 2.17 ));
                }
            });
        }

    };
    private BluetoothGatt connectedGatt;

    @InjectView(R.id.disconnect_button)
    Button disconnectButton;
    @InjectView(R.id.connect_button)
    Button connectButton;
    private boolean connectingToGatt;

    @InjectView(R.id.distance_label)
    TextView distanceLabel;

    private final Object connectingToGattMonitor = new Object();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        bluetooth = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

        ButterKnife.inject(this);

        disconnectButton.setEnabled(false);
    }

    @OnClick(R.id.connect_button)
    protected void connectButtonTapped() {
        final BluetoothAdapter adapter = bluetooth.getAdapter();
        UUID[] serviceUUIDs = new UUID[]{CSC_SERVICE_UUID};
        adapter.startLeScan(new BluetoothAdapter.LeScanCallback() {


            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d(TAG, "found device " + device.getAddress());
                synchronized (connectingToGattMonitor){
                    if (device.getName().contains("BSCBLE V1.5") && !connectingToGatt) {
                        connectingToGatt = true;
                        Log.d(TAG, "connecting to " + device.getAddress());
                        device.connectGatt(Connect.this, false, bluetoothGattCallback);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectButton.setEnabled(false);
                            }
                        });

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
}