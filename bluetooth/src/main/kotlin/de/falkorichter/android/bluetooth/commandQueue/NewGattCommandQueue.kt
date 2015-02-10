/**
 *
 */
package de.falkorichter.android.bluetooth.commandQueue

import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import android.util.Log

import java.util.Arrays
import java.util.HashMap
import java.util.LinkedList
import java.util.UUID

import de.falkorichter.android.bluetooth.utils.BluetoothUtil
import de.falkorichter.android.simplebikecomputer.GattListenerProxy

TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class NewGattCommandQueue : GattListenerProxy.PartialListener() {
    public var results: MutableMap<UUID, Map<UUID, ByteArray>> = HashMap()
    private val servicesToProcess: LinkedList<GattCommandServiceGroup>
    private var bluetoothGatt: BluetoothGatt? = null
    private var gattCommandQueueCallback: GattCommandQueueCallback? = null
    private var currentService: GattCommandServiceGroup? = null
    private var startWhenConnected: Boolean = false
    private var currentOperation: GattCommandServiceGroup.CharacteristicOperation? = null
    private var servicesDiscovered: Boolean = false
    private var processingServices: Boolean = false

    {
        this.servicesToProcess = LinkedList<GattCommandServiceGroup>()
    }

    public fun clear() {
        this.servicesToProcess.clear()
        this.cleanup()
    }

    public fun isEmpty(): Boolean {
        return this.servicesToProcess.isEmpty()
    }

    public fun executeCommands(bluetoothGatt: BluetoothGatt, gattCommandQueueCallback: GattCommandQueueCallback) {
        this.bluetoothGatt = bluetoothGatt
        this.gattCommandQueueCallback = gattCommandQueueCallback

        startProcessServices()
    }

    private fun startProcessServices() {
        if (processingServices) {
            Log.w(TAG, "was already processing Services. Called twice. ")
            return
        }
        this.processingServices = true
        processServices()
    }

    private fun processServices() {
        this.currentService = servicesToProcess.poll()

        if (this.currentService == null) {
            this.gattCommandQueueCallback!!.finishProcessingCommands(this, GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_SUCCESS, null)
            this.cleanup()
            return
        }
        results.put(currentService!!.uuid, HashMap<UUID, ByteArray>())

        if (!this.currentService!!.resolveService(this.bluetoothGatt)) {
            cleanupWithError()
            return
        }

        this.processCharacteristics()
    }


    private fun processCharacteristics() {
        currentOperation = this.currentService!!.pollCharacteristicOperation()
        if (currentOperation == null) {
            processingServices = false
            processServices()
            return
        }

        Log.d(TAG, "executing Characteristic : " + currentOperation!!.uuid.toString())
        if (!currentOperation!!.execute(this.currentService, bluetoothGatt)) {
            if (currentOperation!!.failOnError) {
                this.gattCommandQueueCallback!!.finishProcessingCommands(this, GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_ERROR, bluetoothGatt)
                this.cleanup()
                return
            } else {
                processCharacteristics()
            }
        }
    }

    private fun cleanup() {
        this.bluetoothGatt = null
        this.gattCommandQueueCallback = null
        this.processingServices = false
    }

    private fun cleanupWithError() {
        this.gattCommandQueueCallback!!.finishProcessingCommands(this, GattCommandQueueCallback.CALLBACKSTATE.FINISH_WITH_ERROR, null)
        this.cleanup()
    }

    public fun setGattCommandQueueCallback(gattCommandQueueCallback: GattCommandQueueCallback) {
        this.gattCommandQueueCallback = gattCommandQueueCallback
    }

    /* callbacks for read/write operations from {@link BluetoothGattCallback} */

    override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        Log.d(TAG, "onDescriptorRead" + descriptor.getUuid().toString() + " with value " + Arrays.toString(descriptor.getValue()))
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError()
            return
        }
        processCharacteristics()
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
        Log.d(TAG, "onDescriptorWrite" + descriptor.getUuid().toString() + " with value " + Arrays.toString(descriptor.getValue()))
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError()
            return
        }
        processCharacteristics()
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        Log.d(TAG, "read Characteristic" + characteristic.getUuid().toString() + " with value " + Arrays.toString(characteristic.getValue()))
        if (status != BluetoothGatt.GATT_SUCCESS) {
            if (currentOperation!!.failOnError) {
                cleanupWithError()
                return
            }
        } else {
            results.get(currentService!!.uuid).put(characteristic.getUuid(), characteristic.getValue())
        }
        this.processCharacteristics()
    }

    override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
        Log.d(TAG, "wrote Characteristic" + characteristic.getUuid().toString() + " with value " + Arrays.toString(characteristic.getValue()))
        if (status != BluetoothGatt.GATT_SUCCESS) {
            cleanupWithError()
            return
        }
        this.processCharacteristics()
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        when (newState) {
            BluetoothGatt.STATE_CONNECTED -> {
                if (startWhenConnected) {
                    this.bluetoothGatt = gatt
                    if (!servicesDiscovered) {
                        gatt.discoverServices()
                    }
                }
            }
            BluetoothGatt.STATE_DISCONNECTED, BluetoothGatt.STATE_DISCONNECTING -> {
                if (processingServices) {
                    Log.e(TAG, "While reading Characteristics: " + BluetoothUtil.connectionStateToString(newState))
                    cleanupWithError()
                }
            }
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)

        if (startWhenConnected && !servicesDiscovered) {
            this.bluetoothGatt = gatt
            startProcessServices()
        }
        servicesDiscovered = true
    }

    public fun add(service: GattCommandServiceGroup) {
        this.servicesToProcess.add(service)
    }

    public fun executeWhenConnected() {
        startWhenConnected = true
    }

    class object {

        private val TAG = javaClass<NewGattCommandQueue>().getName()
    }
}
