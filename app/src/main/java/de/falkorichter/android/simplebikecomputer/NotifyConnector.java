package de.falkorichter.android.simplebikecomputer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.Context;

import java.util.UUID;

public abstract class NotifyConnector extends BluetoothGattCallback {

    private static final UUID BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final Listener NONE = new Listener() {
        @Override
        public void onRSSIUpdate(int rssi) {

        }

        @Override
        public void onHeartRateConnected(boolean b) {

        }
    };
    private final Object connectingToGattMonitor = new Object();
    private final BluetoothAdapter bluetoothAdapter;
    private boolean connectingToGatt;
    protected Listener listener = NONE;
    private final Context context;
    private BluetoothGatt connectedGatt;
    private final UUID[] serviceUUIDs;
    private final UUID characteristic;
    private final UUID serviceUUID;

    public void disconnect() {
        if(connectedGatt != null) {
            connectedGatt.disconnect();
        }
    }

    public interface Listener {

        void onRSSIUpdate(int rssi);

        void onHeartRateConnected(boolean b);
    }

    public void scanAndAutoConnect() {
        bluetoothAdapter.startLeScan(serviceUUIDs, new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                synchronized (connectingToGattMonitor) {
                    if (!connectingToGatt) {
                        connectingToGatt = true;
                        listener.onRSSIUpdate(rssi);
                        device.connectGatt(context, true, NotifyConnector.this);
                        bluetoothAdapter.stopLeScan(this);
                    }
                }
            }
        });
    }

    public NotifyConnector(BluetoothAdapter adapter, Context context, UUID[] serviceUUIDs, UUID characteristic, UUID serviceUUID) {
        this.bluetoothAdapter = adapter;
        this.context = context;
        this.serviceUUIDs = serviceUUIDs;
        this.characteristic = characteristic;
        this.serviceUUID = serviceUUID;
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
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        gatt.readRemoteRssi();
    }


        @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        BluetoothGattCharacteristic valueCharacteristic = gatt.getService(serviceUUID).getCharacteristic(characteristic);
        boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);

        BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
    }


}
