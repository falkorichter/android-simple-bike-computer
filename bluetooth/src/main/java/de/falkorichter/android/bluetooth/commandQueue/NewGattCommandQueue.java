/**
 *
 */
package de.falkorichter.android.bluetooth.commandQueue;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Build;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import de.falkorichter.android.bluetooth.utils.BluetoothUtil;
import de.falkorichter.android.simplebikecomputer.GattListenerProxy;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NewGattCommandQueue extends GattListenerProxy.PartialListener {

    private static final String TAG = NewGattCommandQueue.class.getName();
    public Map<UUID, Map<UUID, byte[]>> results = new HashMap<UUID, Map<UUID, byte[]>>();
    private LinkedList<GattCommandServiceGroup> servicesToProcess;
    private BluetoothGatt bluetoothGatt;
    private GattCommandQueueCallback gattCommandQueueCallback;
    private GattCommandServiceGroup currentService;
    private boolean startWhenConnected;
    private GattCommandServiceGroup.CharacteristicOperation currentOperation;
    private boolean servicesDiscovered;
    private boolean processingServices;

    public NewGattCommandQueue() {
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

        startProcessServices();
    }

    private void startProcessServices() {
        if (processingServices) {
            Log.w(TAG, "was already processing Services. Called twice. ");
            return;
        }
        this.processingServices = true;
        processServices();
    }

    private void processServices() {
        this.currentService = servicesToProcess.poll();

        if (this.currentService == null) {
            this.gattCommandQueueCallback.finishProcessingCommands(this, GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_SUCCESS, null);
            this.cleanup();
            return;
        }
        results.put(currentService.uuid, new HashMap<UUID, byte[]>());

        if (!this.currentService.resolveService(this.bluetoothGatt)) {
            cleanupWithError();
            return;
        }

        this.processCharacteristics();
    }


    private void processCharacteristics() {
        currentOperation = this.currentService.pollCharacteristicOperation();
        if (currentOperation == null) {
            processingServices = false;
            processServices();
            return;
        }

        Log.d(TAG, "executing Characteristic : " + currentOperation.uuid.toString());
        if (!currentOperation.execute(this.currentService, bluetoothGatt)) {
            if (currentOperation.failOnError) {
                this.gattCommandQueueCallback.finishProcessingCommands(this, GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_ERROR, bluetoothGatt);
                this.cleanup();
                return;
            } else {
                processCharacteristics();
            }
        }
    }

    private void cleanup() {
        this.bluetoothGatt = null;
        this.gattCommandQueueCallback = null;
        this.processingServices = false;
    }

    private void cleanupWithError() {
        this.gattCommandQueueCallback.finishProcessingCommands(this, GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_ERROR, null);
        this.cleanup();
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
        processCharacteristics();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.d(TAG, "onDescriptorWrite" + descriptor.getUuid().toString() + " with value " + Arrays.toString(descriptor.getValue()));
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError();
            return;
        }
        processCharacteristics();
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "read Characteristic" + characteristic.getUuid().toString() + " with value " + Arrays.toString(characteristic.getValue()));
        if (status != BluetoothGatt.GATT_SUCCESS) {
            if (currentOperation.failOnError) {
                cleanupWithError();
                return;
            }
        } else {
            results.get(currentService.uuid).put(characteristic.getUuid(), characteristic.getValue());
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
                if (startWhenConnected) {
                    this.bluetoothGatt = gatt;
                    if (!servicesDiscovered) {
                        gatt.discoverServices();
                    }
                }
                break;
            }
            case BluetoothGatt.STATE_DISCONNECTED:
            case BluetoothGatt.STATE_DISCONNECTING: {
                if (processingServices) {
                    Log.e(TAG, "While reading Characteristics: " + BluetoothUtil.connectionStateToString(newState));
                    cleanupWithError();
                }
                break;
            }
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if (startWhenConnected && !servicesDiscovered) {
            this.bluetoothGatt = gatt;
            startProcessServices();
        }
        servicesDiscovered = true;
    }

    public void add(GattCommandServiceGroup service) {
        this.servicesToProcess.add(service);
    }

    public void executeWhenConnected() {
        startWhenConnected = true;
    }
}
