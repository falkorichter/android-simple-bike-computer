package de.falkorichter.android.bluetooth.utils

import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothProfile
import android.os.Build

TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothUtil {
    class object {

        public fun connectionStateToString(state: Int): String {
            when (state) {
                BluetoothProfile.STATE_CONNECTED -> return "STATE_CONNECTED"
                BluetoothProfile.STATE_DISCONNECTED -> return "STATE_DISCONNECTED"
                BluetoothProfile.STATE_CONNECTING -> return "STATE_CONNECTING"
                BluetoothProfile.STATE_DISCONNECTING -> return "STATE_DISCONNECTING"
                else -> return "unknown state:" + state
            }
        }

        public fun statusToString(status: Int): String {
            when (status) {
                BluetoothGatt.GATT_SUCCESS -> return "GATT_SUCCESS"
                BluetoothGatt.GATT_READ_NOT_PERMITTED -> return "GATT_READ_NOT_PERMITTED"
                BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> return "GATT_WRITE_NOT_PERMITTED"
                BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> return "GATT_REQUEST_NOT_SUPPORTED"
                BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> return "GATT_INSUFFICIENT_AUTHENTICATION"
                BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> return "GATT_INSUFFICIENT_ENCRYPTION"
                BluetoothGatt.GATT_INVALID_OFFSET -> return "GATT_INVALID_OFFSET"
                BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> return "GATT_INVALID_ATTRIBUTE_LENGTH"
                BluetoothGatt.GATT_FAILURE -> return "GATT_FAILURE"
                else -> return "unknown state:" + status
            }
        }

        public fun gattToString(gatt: BluetoothGatt?): String {
            if (gatt == null) {
                return "null"
            }
            return "gatt:" + gatt.getDevice().getName()
        }
    }
}
