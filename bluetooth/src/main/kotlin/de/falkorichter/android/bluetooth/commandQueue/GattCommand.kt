package de.falkorichter.android.bluetooth.commandQueue

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor

public fun GattCommand(bluetoothGattDescriptor: BluetoothGattDescriptor, commandOperation: CommandOperation): GattCommand {
    return GattCommand(GattCommand.CommandType.TYPE_DESCRIPTOR, commandOperation, bluetoothGattDescriptor, null)
}

public fun GattCommand(bluetoothGattCharacteristic: BluetoothGattCharacteristic, commandOperation: CommandOperation): GattCommand {
    return GattCommand(GattCommand.CommandType.TYPE_CHARACTERISTIC, commandOperation, null, bluetoothGattCharacteristic)
}

public class GattCommand(
        public val commandType: CommandType, public val commandOperation: CommandOperation, public val bluetoothGattDescriptor: BluetoothGattDescriptor, public val bluetoothGattCharacteristic: BluetoothGattCharacteristic) {

    public enum class CommandType {
        TYPE_DESCRIPTOR
        TYPE_CHARACTERISTIC
    }

    public enum class CommandOperation {
        OPERATION_READ
        CommandOperation
        OPERATION_WRITE
    }
}
