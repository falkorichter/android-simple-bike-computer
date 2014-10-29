package de.falkorichter.android.bluetooth.commandQueue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class GattCommand {

    public enum CommandType {
        TYPE_DESCRIPTOR,
        TYPE_CHARACTERISTIC
    }

    public enum CommandOperation {
        OPERATION_READ,
        CommandOperation, OPERATION_WRITE
    }

    public final CommandType commandType;
    public final CommandOperation commandOperation;
    private BluetoothGattDescriptor bluetoothGattDescriptor;
    private BluetoothGattCharacteristic bluetoothGattCharacteristic;

    public GattCommand(BluetoothGattDescriptor bluetoothGattDescriptor, CommandOperation commandOperation) {
        this.bluetoothGattDescriptor = bluetoothGattDescriptor;
        this.commandOperation = commandOperation;
        this.commandType = CommandType.TYPE_DESCRIPTOR;
    }

    public GattCommand(BluetoothGattCharacteristic bluetoothGattCharacteristic, CommandOperation commandOperation) {
        this.bluetoothGattCharacteristic = bluetoothGattCharacteristic;
        this.commandOperation = commandOperation;
        this.commandType = CommandType.TYPE_CHARACTERISTIC;
    }

    public BluetoothGattDescriptor getBluetoothGattDescriptor() {
        return this.bluetoothGattDescriptor;
    }

    public BluetoothGattCharacteristic getBluetoothGattCharacteristic() {
        return this.bluetoothGattCharacteristic;
    }
}
