package de.falkorichter.android.bluetooth.commandQueue;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Build;

import java.util.LinkedList;
import java.util.UUID;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class GattCommandServiceGroup {
    public final UUID uuid;
    private final LinkedList<CharacteristicOperation> operations;
    private BluetoothGattService service;

    public GattCommandServiceGroup(UUID uuid) {
        this.uuid = uuid;
        operations = new LinkedList<CharacteristicOperation>();
    }

    public void addCharacteristicOperation(GattCommand.CommandOperation operation, UUID uuid, byte[] value) {
        this.operations.add(new CharacteristicOperation(operation, uuid, value, true));
    }

    public boolean resolveService(BluetoothGatt bluetoothGatt) {
        this.service = bluetoothGatt.getService(uuid);
        return service != null;
    }

    public CharacteristicOperation pollCharacteristicOperation() {
        return operations.poll();
    }

    public void addCharacteristicOperation(GattCommand.CommandOperation operation, UUID uuid, boolean failOnError) {
        this.operations.add(new CharacteristicOperation(operation, uuid, null, failOnError));
    }

    public void addCharacteristicOperation(GattCommand.CommandOperation operation, UUID uuid) {
        addCharacteristicOperation(operation, uuid, false);
    }

    public class CharacteristicOperation {
        public final GattCommand.CommandOperation operation;
        public final UUID uuid;
        public final byte[] value;
        public final boolean failOnError;

        private BluetoothGattCharacteristic characteristic;

        public CharacteristicOperation(GattCommand.CommandOperation operation, UUID uuid, byte[] value, boolean failOnError) {
            this.operation = operation;
            this.uuid = uuid;
            this.value = value;
            this.failOnError = failOnError;
        }

        public boolean execute(GattCommandServiceGroup gattCommandData, BluetoothGatt bluetoothGatt) {
            characteristic = gattCommandData.service.getCharacteristic(uuid);
            if (characteristic == null) {
                return false;
            }
            if (operation == GattCommand.CommandOperation.OPERATION_READ) {
                bluetoothGatt.readCharacteristic(characteristic);
            } else {
                characteristic.setValue(value);
                bluetoothGatt.writeCharacteristic(characteristic);
            }
            return true;
        }
    }
}
