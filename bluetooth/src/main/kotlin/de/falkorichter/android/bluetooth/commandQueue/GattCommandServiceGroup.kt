package de.falkorichter.android.bluetooth.commandQueue

import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Build

import java.util.LinkedList
import java.util.UUID

TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattCommandServiceGroup(public val uuid: UUID) {
    private val operations: LinkedList<CharacteristicOperation>
    private var service: BluetoothGattService? = null

    {
        operations = LinkedList<CharacteristicOperation>()
    }

    public fun addCharacteristicOperation(operation: GattCommand.CommandOperation, uuid: UUID, value: ByteArray) {
        this.operations.add(CharacteristicOperation(operation, uuid, value, true))
    }

    public fun resolveService(bluetoothGatt: BluetoothGatt): Boolean {
        this.service = bluetoothGatt.getService(uuid)
        return service != null
    }

    public fun pollCharacteristicOperation(): CharacteristicOperation {
        return operations.poll()
    }

    public fun addCharacteristicOperation(operation: GattCommand.CommandOperation, uuid: UUID, failOnError: Boolean) {
        this.operations.add(CharacteristicOperation(operation, uuid, null, failOnError))
    }

    public fun addCharacteristicOperation(operation: GattCommand.CommandOperation, uuid: UUID) {
        addCharacteristicOperation(operation, uuid, false)
    }

    public inner class CharacteristicOperation(public val operation: GattCommand.CommandOperation, public val uuid: UUID, public val value: ByteArray, public val failOnError: Boolean) {

        private var characteristic: BluetoothGattCharacteristic? = null

        public fun execute(gattCommandData: GattCommandServiceGroup, bluetoothGatt: BluetoothGatt): Boolean {
            characteristic = gattCommandData.service!!.getCharacteristic(uuid)
            if (characteristic == null) {
                return false
            }
            if (operation == GattCommand.CommandOperation.OPERATION_READ) {
                bluetoothGatt.readCharacteristic(characteristic)
            } else {
                characteristic!!.setValue(value)
                bluetoothGatt.writeCharacteristic(characteristic)
            }
            return true
        }
    }
}
