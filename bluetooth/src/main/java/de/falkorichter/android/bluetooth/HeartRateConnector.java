package de.falkorichter.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import java.util.UUID;

public class HeartRateConnector extends NotifyConnector {

    private static final UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    private static final UUID HR_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    public interface HeartRateListener extends NotifyConnector.Listener{
        void heartRateChanged(int heartRateMeasurementValue);
    }

    public HeartRateConnector(BluetoothAdapter adapter, Context connect) {
        super(adapter, connect, new UUID[]{HR_SERVICE_UUID}, HR_MEASUREMENT_CHARACTERISTIC_UUID, HR_SERVICE_UUID);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);

        byte[] value = characteristic.getValue();
        final int heartRateMeasurementValue         = (value[1] & 0xff);
        getHeartRateListener().heartRateChanged(heartRateMeasurementValue);
    }

    private HeartRateListener getHeartRateListener() {
        return (HeartRateListener) listener;
    }

    public void setListener(HeartRateListener connectorListener) {
        listener = connectorListener;
    }
}
