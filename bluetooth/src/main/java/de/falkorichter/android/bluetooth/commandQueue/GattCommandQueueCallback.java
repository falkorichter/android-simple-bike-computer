/**
 *
 */
package de.falkorichter.android.bluetooth.commandQueue;

import android.bluetooth.BluetoothGatt;

public interface GattCommandQueueCallback {
    public void finishProcessingCommands(NewGattCommandQueue queue, CALLBACKSTATE callbackstate, BluetoothGatt bluetoothGatt);

    public enum CALLBACKSTATE {
        FINISH_WITH_SUCCESS,
        FINISH_WITH_ERROR
    }
}
