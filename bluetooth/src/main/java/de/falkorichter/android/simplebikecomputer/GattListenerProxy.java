package de.falkorichter.android.simplebikecomputer;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.falkorichter.android.bluetooth.utils.BluetoothUtil;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattListenerProxy extends BluetoothGattCallback {
    List<Listener> listenerList = new ArrayList<Listener>();

    public GattListenerProxy() {
        super();
    }

    public void addListener(Listener listener) {
        listenerList.add(listener);
    }

    public void removeListener(Listener listener) {
        listenerList.remove(listener);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        for (Listener listener : listenerList) {
            listener.onConnectionStateChange(gatt, status, newState);
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        for (Listener listener : listenerList) {
            listener.onServicesDiscovered(gatt, status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        for (Listener listener : listenerList) {
            listener.onCharacteristicRead(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        for (Listener listener : listenerList) {
            listener.onCharacteristicWrite(gatt, characteristic, status);
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        for (Listener listener : listenerList) {
            listener.onCharacteristicChanged(gatt, characteristic);
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);
        for (Listener listener : listenerList) {
            listener.onDescriptorRead(gatt, descriptor, status);
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        for (Listener listener : listenerList) {
            listener.onDescriptorWrite(gatt, descriptor, status);
        }
    }

    @Override
    public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
        super.onReliableWriteCompleted(gatt, status);
        for (Listener listener : listenerList) {
            listener.onReliableWriteCompleted(gatt, status);
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        for (Listener listener : listenerList) {
            listener.onReadRemoteRssi(gatt, rssi, status);
        }
    }

    public interface Listener {

        void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);


        void onServicesDiscovered(BluetoothGatt gatt, int status);


        void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);


        void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);


        void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);


        void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);


        void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);


        void onReliableWriteCompleted(BluetoothGatt gatt, int status);


        void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);
    }

    public static class LogListener extends PartialListener {
        private static String TAG = LogListener.class.getSimpleName();

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange: " +
                    " status:" + BluetoothUtil.statusToString(status) +
                    " newState:" + BluetoothUtil.connectionStateToString(newState));
        }
    }

    public static class PartialListener implements Listener {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

        }
    }
}
