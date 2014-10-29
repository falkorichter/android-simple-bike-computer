/**
 *
 */
package de.falkorichter.android.bluetooth.commandQueue;

public interface GattCommandQueueCallback {
    public enum CALLBACKSTATE {
        FINISH_WITH_SUCCESS,
        FINISH_WITH_ERROR
    }

    public void finishProcessingCommands(CALLBACKSTATE callbackstate);
}
