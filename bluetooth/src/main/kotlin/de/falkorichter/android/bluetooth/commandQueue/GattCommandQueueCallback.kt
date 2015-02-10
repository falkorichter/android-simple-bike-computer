/**
 *
 */
package de.falkorichter.android.bluetooth.commandQueue

import android.bluetooth.BluetoothGatt

public trait GattCommandQueueCallback {
    public fun finishProcessingCommands(queue: NewGattCommandQueue, callbackstate: CALLBACKSTATE, bluetoothGatt: BluetoothGatt)

    public enum class CALLBACKSTATE {
        FINISH_WITH_SUCCESS
        FINISH_WITH_ERROR
    }
}
