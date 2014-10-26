package de.falkorichter.android.simplebikecomputer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import java.util.UUID;

public class HeartRateConnector extends BluetoothGattCallback {

    private static final UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");
    private static final UUID BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final Listener NONE = new Listener() {
        @Override
        public void onRSSIUpdate(int rssi) {

        }

        @Override
        public void heartRateChanged(int heartRateMeasurementValue) {

        }

        @Override
        public void onHeartRateConnected(boolean b) {

        }
    };
    private final Object connectingToGattMonitor = new Object();
    private final BluetoothAdapter bluetoothAdapter;
    private boolean connectingToGatt;
    private Listener listener = NONE;
    private final Context context;
    private BluetoothGatt connectedGatt;

    public void disconnect() {
        if(connectedGatt != null) {
            connectedGatt.disconnect();
        }
    }

    interface Listener {

        void onRSSIUpdate(int rssi);

        void heartRateChanged(int heartRateMeasurementValue);

        void onHeartRateConnected(boolean b);
    }

    public void scanAndAutoConnect() {
        UUID[] serviceUUIDs = new UUID[]{HR_SERVICE_UUID};
        bluetoothAdapter.startLeScan(serviceUUIDs, new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                synchronized (connectingToGattMonitor) {
                    if (/*device.getName().contains("BSCBLE V1.5") && */!connectingToGatt) {
                        connectingToGatt = true;
                        listener.onRSSIUpdate(rssi);
                        device.connectGatt(context, true, HeartRateConnector.this);
                        bluetoothAdapter.stopLeScan(this);
                    }
                }
            }
        });
    }

    public HeartRateConnector(BluetoothAdapter adapter, Context context) {
        this.bluetoothAdapter = adapter;
        this.context = context;
    }

    public void setListener(Listener connectorListener) {
        listener = connectorListener;
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
        connectingToGatt = false;
        super.onConnectionStateChange(gatt, status, state);

        switch (state) {
            case BluetoothGatt.STATE_CONNECTED: {
                gatt.discoverServices();
                listener.onHeartRateConnected(true);
                this.connectedGatt = gatt;
                break;
            }
            case BluetoothGatt.STATE_DISCONNECTED:
                listener.onHeartRateConnected(false);
                this.connectedGatt = null;
                break;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        BluetoothGattCharacteristic valueCharacteristic = gatt.getService(HR_SERVICE_UUID).getCharacteristic(HR_MEASUREMENT_CHARACTERISTIC_UUID);
        boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);

        BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        byte[] value = characteristic.getValue();
        final int heartRateMeasurementValue         = (value[1] & 0xff);
        this.listener.heartRateChanged(heartRateMeasurementValue);

    }
}
