package de.falkorichter.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.util.Log;

import java.util.UUID;


public class SpeedAndCadenceConnector extends NotifyConnector {

    private static final String TAG = SpeedAndCadenceConnector.class.getSimpleName();

    private static final UUID CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    private static final UUID CSC_CHARACTERISTIC_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

    public static final int NOT_SET = Integer.MIN_VALUE;
    double lastWheelTime = NOT_SET;
    long lastWheelCount = NOT_SET;
    double wheelSize = 2.17;


    public interface SpeedAndCadenceConnectorListener extends NotifyConnector.Listener{
        void speedChanged(double speedInKilometersPerHour);
    }

    public SpeedAndCadenceConnector(BluetoothAdapter adapter, Context connect) {
        super(adapter, connect, new UUID[]{CSC_SERVICE_UUID}, CSC_CHARACTERISTIC_UUID, CSC_SERVICE_UUID);
    }

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


        long numberOfWheelRevolutions = cumulativeWheelRevolutions - lastWheelCount;

        if (lastWheelTime  != lastWheelEventTime && numberOfWheelRevolutions > 0){
            double timeDiff = lastWheelEventTime - lastWheelTime;

            double speedinMetersPerSeconds = (wheelSize * numberOfWheelRevolutions) / timeDiff;
            double speedInKilometersPerHour = speedinMetersPerSeconds * 3.6;

            getSpeedAndCadenceConnectorListener().speedChanged(speedInKilometersPerHour);
            Log.d(TAG, "speed:" + speedInKilometersPerHour);

            lastWheelCount = cumulativeWheelRevolutions;
            lastWheelTime = lastWheelEventTime;
        }

        gatt.readRemoteRssi();
    }

    private SpeedAndCadenceConnectorListener getSpeedAndCadenceConnectorListener() {
        return (SpeedAndCadenceConnectorListener) listener;
    }

    public void setListener(SpeedAndCadenceConnectorListener connectorListener) {
        listener = connectorListener;
    }
}