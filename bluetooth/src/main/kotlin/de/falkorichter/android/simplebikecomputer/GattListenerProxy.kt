package de.falkorichter.android.simplebikecomputer

import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import android.util.Log

import java.util.ArrayList

import de.falkorichter.android.bluetooth.utils.BluetoothUtil

TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattListenerProxy : BluetoothGattCallback() {
    var listenerList: MutableList<Listener> = ArrayList()

    public fun addListener(listener: Listener) {
        listenerList.add(listener)
    }

    public fun removeListener(listener: Listener) {
        listenerList.remove(listener)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        for (listener in listenerList) {
            listener.onConnectionStateChange(gatt, status, newState)
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        for (listener in listenerList) {
            listener.onServicesDiscovered(gatt, status)
        }
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        for (listener in listenerList) {
            listener.onCharacteristicRead(gatt, characteristic, status)
        }
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)
        for (listener in listenerList) {
            listener.onCharacteristicWrite(gatt, characteristic, status)
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)
        for (listener in listenerList) {
            listener.onCharacteristicChanged(gatt, characteristic)
        }
    }

    override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        super.onDescriptorRead(gatt, descriptor, status)
        for (listener in listenerList) {
            listener.onDescriptorRead(gatt, descriptor, status)
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        for (listener in listenerList) {
            listener.onDescriptorWrite(gatt, descriptor, status)
        }
    }

    override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
        super.onReliableWriteCompleted(gatt, status)
        for (listener in listenerList) {
            listener.onReliableWriteCompleted(gatt, status)
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        for (listener in listenerList) {
            listener.onReadRemoteRssi(gatt, rssi, status)
        }
    }

    public trait Listener {

        public fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)


        public fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)


        public fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int)


        public fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int)


        public fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic)


        public fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int)


        public fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int)


        public fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int)


        public fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int)
    }

    public class LogListener : PartialListener() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.d(TAG, "onConnectionStateChange: " + " status:" + BluetoothUtil.statusToString(status) + " newState:" + BluetoothUtil.connectionStateToString(newState))
        }

        class object {
            private val TAG = javaClass<LogListener>().getSimpleName()
        }
    }

    public open class PartialListener : Listener {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        }

        override fun onReliableWriteCompleted(gatt: BluetoothGatt, status: Int) {
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
        }
    }
}
