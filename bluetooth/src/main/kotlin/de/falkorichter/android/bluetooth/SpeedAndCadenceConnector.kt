package de.falkorichter.android.bluetooth

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build
import android.util.Log

import java.util.UUID

TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class SpeedAndCadenceConnector(adapter: BluetoothAdapter, connect: Context) : NotifyConnector(adapter, connect, array(SpeedAndCadenceConnector.CSC_SERVICE_UUID), SpeedAndCadenceConnector.CSC_CHARACTERISTIC_UUID, SpeedAndCadenceConnector.CSC_SERVICE_UUID) {
    var lastWheelTime = NOT_SET.toDouble()
    var lastWheelCount = NOT_SET.toLong()
    var wheelSize = 2.17

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)
        val value = characteristic.getValue()

        val cumulativeWheelRevolutions = (value[1] and 255) or ((value[2] and 255) shl 8) or ((value[3] and 255) shl 16) or ((value[4] and 255) shl 24).toLong()
        val lastWheelEventReadValue = (value[5] and 255) or ((value[6] and 255) shl 8)
        val cumulativeCrankRevolutions = (value[7] and 255) or ((value[8] and 255) shl 8)
        val lastCrankEventReadValue = (value[9] and 255) or ((value[10] and 255) shl 8)

        val lastWheelEventTime = lastWheelEventReadValue.toDouble() / 1024.0

        Log.d(TAG, "onCharacteristicChanged " + cumulativeWheelRevolutions + ":" + lastWheelEventReadValue + ":" + cumulativeCrankRevolutions + ":" + lastCrankEventReadValue)

        if (lastWheelTime == NOT_SET) {
            lastWheelTime = lastWheelEventTime
        }
        if (lastWheelCount == NOT_SET) {
            lastWheelCount = cumulativeWheelRevolutions
        }


        val numberOfWheelRevolutions = cumulativeWheelRevolutions - lastWheelCount

        getSpeedAndCadenceConnectorListener().onTotalDistanceChanged(numberOfWheelRevolutions.toDouble() * wheelSize)

        if (lastWheelTime != lastWheelEventTime && numberOfWheelRevolutions > 0) {
            val timeDiff = lastWheelEventTime - lastWheelTime

            val speedinMetersPerSeconds = (wheelSize * numberOfWheelRevolutions.toDouble()) / timeDiff
            val speedInKilometersPerHour = speedinMetersPerSeconds * 3.6

            getSpeedAndCadenceConnectorListener().speedChanged(speedInKilometersPerHour)
            Log.d(TAG, "speed:" + speedInKilometersPerHour)

            lastWheelCount = cumulativeWheelRevolutions
            lastWheelTime = lastWheelEventTime
        }

        gatt.readRemoteRssi()
    }

    private fun getSpeedAndCadenceConnectorListener(): SpeedAndCadenceConnectorListener {
        return listener as SpeedAndCadenceConnectorListener
    }

    public fun setListener(connectorListener: SpeedAndCadenceConnectorListener) {
        listener = connectorListener
    }

    public trait SpeedAndCadenceConnectorListener : NotifyConnector.Listener {
        public fun speedChanged(speedInKilometersPerHour: Double)

        public fun onTotalDistanceChanged(totalDistanceInMeters: Double)
    }

    class object {

        public val NOT_SET: Int = Integer.MIN_VALUE
        private val TAG = javaClass<SpeedAndCadenceConnector>().getSimpleName()
        private val CSC_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb")
        private val CSC_CHARACTERISTIC_UUID = UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb")
    }
}