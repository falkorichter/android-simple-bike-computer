package de.falkorichter.android.bluetooth

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Build

import java.util.UUID

TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class HeartRateConnector(adapter: BluetoothAdapter, connect: Context) : NotifyConnector(adapter, connect, array<UUID>(HeartRateConnector.HR_SERVICE_UUID), HeartRateConnector.HR_MEASUREMENT_CHARACTERISTIC_UUID, HeartRateConnector.HR_SERVICE_UUID) {

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)

        val value = characteristic.getValue()
        val heartRateMeasurementValue = (value[1] and 255)
        getHeartRateListener().heartRateChanged(heartRateMeasurementValue)
    }

    private fun getHeartRateListener(): HeartRateListener {
        return listener as HeartRateListener
    }

    public fun setListener(connectorListener: HeartRateListener) {
        listener = connectorListener
    }

    public trait HeartRateListener : NotifyConnector.Listener {
        public fun heartRateChanged(heartRateMeasurementValue: Int)
    }

    class object {

        private val HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb")
        private val HR_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")
    }
}
