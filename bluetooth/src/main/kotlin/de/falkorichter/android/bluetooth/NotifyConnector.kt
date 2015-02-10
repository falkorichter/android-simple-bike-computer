package de.falkorichter.android.bluetooth

import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.os.Build
import android.util.Log

import java.util.UUID

import de.falkorichter.android.bluetooth.utils.BluetoothUtil

TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public abstract class NotifyConnector(private val bluetoothAdapter: BluetoothAdapter,
                                      private val context: Context,
                                      private val serviceUUIDs: Array<UUID>,
                                      private val characteristic: UUID,
                                      private val serviceUUID: UUID) : BluetoothGattCallback() {
    protected var listener: Listener = NONE
    private val connectingToGattMonitor = Object()
    private var connectingToGatt: Boolean = false
    private var connectedGatt: BluetoothGatt? = null
    private var state = ConnectionState.disconnected

    public fun disconnect() {
        if (connectedGatt != null) {
            connectedGatt!!.disconnect()
        }
        connectingToGatt = false
        setCurrentState(ConnectionState.disconnected)
    }

    public fun isConnecting(): Boolean {
        return connectingToGatt
    }

    public fun isConnected(): Boolean {
        return connectedGatt != null
    }

    public fun scanAndAutoConnect() {
        if (state == ConnectionState.disconnected) {
            setCurrentState(ConnectionState.scanning)
            bluetoothAdapter.startLeScan(serviceUUIDs, object : BluetoothAdapter.LeScanCallback {
                override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray) {
                    synchronized (connectingToGattMonitor) {
                        if (!connectingToGatt) {
                            setCurrentState(ConnectionState.connecting)
                            connectingToGatt = true
                            listener.onRSSIUpdate(this@NotifyConnector, rssi)
                            device.connectGatt(context, true, this@NotifyConnector)
                            bluetoothAdapter.stopLeScan(this)
                        }
                    }
                }
            })
        } else {
            Log.e(TAG, "cannot connect, in state:" + connectionStateString(state))
        }
    }

    private fun setCurrentState(state: ConnectionState) {
        this.state = state
        listener.onConnectionStateChanged(this@NotifyConnector, state)
    }

    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        Log.d(TAG, "onConnectionStateChange:" + gatt + " status:" + BluetoothUtil.statusToString(status) + " newState:" + BluetoothUtil.connectionStateToString(newState) + " NotifyConnector.state:" + connectionStateString(state))

        when (newState) {
            BluetoothGatt.STATE_CONNECTING -> {
                if (state != ConnectionState.connecting) {
                    Log.e(TAG, "onConnectionStateChange connectingConfirmed  when state was not connecting: " + connectionStateString(state))
                }
                setCurrentState(ConnectionState.connectingConfirmed)
            }
            BluetoothGatt.STATE_CONNECTED -> {
                if (state == ConnectionState.connectingConfirmed || state == ConnectionState.connecting) {
                    connectingToGatt = false
                    gatt.discoverServices()
                    setCurrentState(ConnectionState.connected)
                    this.connectedGatt = gatt
                } else {
                    Log.e(TAG, "onConnectionStateChange when the NotifiyConnector was in state " + connectionStateString(state))
                }
            }
            BluetoothGatt.STATE_DISCONNECTING -> setCurrentState(ConnectionState.disconnecting)
            BluetoothGatt.STATE_DISCONNECTED -> {
                setCurrentState(ConnectionState.disconnected)
                this.connectedGatt = null
            }
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)
        gatt.readRemoteRssi()
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)

        val valueCharacteristic = gatt.getService(serviceUUID).getCharacteristic(characteristic)
        val notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true)

        val descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID)
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        val writeDescriptorSuccess = gatt.writeDescriptor(descriptor)
    }

    public enum class ConnectionState {
        disconnected
        scanning
        connecting
        connected
        connectingConfirmed
        disconnecting
    }


    public trait Listener {

        public fun onRSSIUpdate(connector: NotifyConnector, rssi: Int)

        public fun onConnectionStateChanged(connector: NotifyConnector, connectionState: ConnectionState)
    }

    class object {
        private val TAG = javaClass<NotifyConnector>().getSimpleName()

        private val BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private val NONE = object : Listener {

            override fun onRSSIUpdate(connector: NotifyConnector, rssi: Int) {
            }

            override fun onConnectionStateChanged(connector: NotifyConnector, connectionState: ConnectionState) {
            }
        }

        public fun connectionStateString(state: ConnectionState): String {
            when (state) {
                NotifyConnector.ConnectionState.disconnected -> return "disconnected"
                NotifyConnector.ConnectionState.scanning -> return "scanning"
                NotifyConnector.ConnectionState.connecting -> return "connecting"
                NotifyConnector.ConnectionState.connected -> return "connected"
                NotifyConnector.ConnectionState.connectingConfirmed -> return "connectingConfirmed"
                NotifyConnector.ConnectionState.disconnecting -> return "disconnecting"
                else -> return "unknown"
            }
        }
    }


}
