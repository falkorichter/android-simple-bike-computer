/**
 *
 */
package de.falkorichter.android.bluetooth.commandQueue;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;

public class GattCommandQueue extends BluetoothGattCallback {
    private static final String TAG = GattCommandQueue.class.getName();

    private LinkedList<GattCommandServiceGroup> servicesToProcess;
    private BluetoothGatt bluetoothGatt;
    private GattCommandQueueCallback gattCommandQueueCallback;
    private GattCommandServiceGroup currentService;
    private boolean startWhenConnected;

    public GattCommandQueue() {
        this.servicesToProcess = new LinkedList<GattCommandServiceGroup>();
    }

    public void clear() {
        this.servicesToProcess.clear();
        this.cleanup();
    }

    public boolean isEmpty() {
        return this.servicesToProcess.isEmpty();
    }

    public void executeCommands(BluetoothGatt bluetoothGatt, GattCommandQueueCallback gattCommandQueueCallback) {
        this.bluetoothGatt = bluetoothGatt;
        this.gattCommandQueueCallback = gattCommandQueueCallback;

        processServices();
    }

    private void processServices() {

        this.currentService = servicesToProcess.poll();
        if (this.currentService == null) {
            if (this.gattCommandQueueCallback != null)
                this.gattCommandQueueCallback.finishProcessingCommands(GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_SUCCESS);
            this.cleanup();
            return;
        }

        if (!this.currentService.resolveService(this.bluetoothGatt)) {
            cleanupWithError();
            return;
        }

        this.processCharacteristics();
    }

    private void cleanupWithError() {
        if (this.gattCommandQueueCallback != null)
            this.gattCommandQueueCallback.finishProcessingCommands(GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_ERROR);
        this.cleanup();
    }

    private void processCharacteristics() {

        GattCommandServiceGroup.CharacteristicOperation operation = this.currentService.pollCharacteristicOperation();
        if (operation == null) {
            processServices();
            return;
        }

        if (!operation.execute(this.currentService, bluetoothGatt)) {
            if (this.gattCommandQueueCallback != null)
                this.gattCommandQueueCallback.finishProcessingCommands(GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_SUCCESS);
            this.cleanup();
            return;
        }

    }

    private void cleanup() {
        this.bluetoothGatt = null;
        this.gattCommandQueueCallback = null;
    }

    public void setGattCommandQueueCallback(GattCommandQueueCallback gattCommandQueueCallback) {
        this.gattCommandQueueCallback = gattCommandQueueCallback;
    }

	/* callbacks for read/write operations from {@link BluetoothGattCallback} */

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.d(TAG, "onDescriptorRead" + descriptor.getUuid().toString() + " with value " + Arrays.toString(descriptor.getValue()));
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError();
            return;
        }
        processServices();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.d(TAG, "onDescriptorWrite" + descriptor.getUuid().toString() + " with value " + Arrays.toString(descriptor.getValue()));
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError();
            return;
        }
        processServices();
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "read Characteristic" + characteristic.getUuid().toString() + " with value " + Arrays.toString(characteristic.getValue()));
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError();
            return;
        }
        this.processCharacteristics();
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "wrote Characteristic" + characteristic.getUuid().toString() + " with value " + Arrays.toString(characteristic.getValue()));
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError();
            return;
        }
        this.processCharacteristics();
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        switch (newState) {
            case BluetoothGatt.STATE_CONNECTED: {
                if(startWhenConnected) {
                    this.bluetoothGatt = gatt;
                    gatt.discoverServices();
                }
                break;
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if (startWhenConnected) {
            processServices();
        }
    }

    public void add(GattCommandServiceGroup service) {
        this.servicesToProcess.add(service);
    }

    public void executeWhenConnected() {
        startWhenConnected = true;
    }
}
