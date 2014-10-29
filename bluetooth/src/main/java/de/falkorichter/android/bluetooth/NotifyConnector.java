package de.falkorichter.android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import de.falkorichter.android.bluetooth.utils.BluetoothUtil;

public abstract class NotifyConnector extends BluetoothGattCallback {
    private static final String TAG = NotifyConnector.class.getSimpleName();

    private static final UUID BTLE_NOTIFICATION_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final Listener NONE = new Listener() {

        @Override
        public void onRSSIUpdate(NotifyConnector connector, int rssi) {

        }

        @Override
        public void onConnectionStateChanged(NotifyConnector connector, ConnectionState connectionState) {

        }
    };
    private final Object connectingToGattMonitor = new Object();
    private final BluetoothAdapter bluetoothAdapter;
    private boolean connectingToGatt;
    protected Listener listener = NONE;
    private final Context context;
    private BluetoothGatt connectedGatt;
    private final UUID[] serviceUUIDs;
    private final UUID characteristic;
    private final UUID serviceUUID;

    private ConnectionState state = ConnectionState.disconnected;

    public enum ConnectionState {
        disconnected,
        scanning,
        connecting,
        connected,
        connectingConfirmed,
        disconnecting
    }

    public static String connectionStateString(ConnectionState state) {
        switch (state) {
            case disconnected:
                return "disconnected";
            case scanning:
                return "scanning";
            case connecting:
                return "connecting";
            case connected:
                return "connected";
            case connectingConfirmed:
                return "connectingConfirmed";
            case disconnecting:
                return "disconnecting";
            default:
                return "unknown";

        }
    }

    public void disconnect() {
        if (connectedGatt != null) {
            connectedGatt.disconnect();
        }
        connectingToGatt = false;
        setCurrentState(ConnectionState.disconnected);
    }

    public boolean isConnecting() {
        return connectingToGatt;
    }

    public boolean isConnected() {
        return connectedGatt != null;
    }

    public interface Listener {

        void onRSSIUpdate(NotifyConnector connector, int rssi);

        void onConnectionStateChanged(NotifyConnector connector, ConnectionState connectionState);
    }

    public void scanAndAutoConnect() {
        if (state == ConnectionState.disconnected) {
            setCurrentState(ConnectionState.scanning);
            bluetoothAdapter.startLeScan(serviceUUIDs, new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    synchronized (connectingToGattMonitor) {
                        if (!connectingToGatt) {
                            setCurrentState(ConnectionState.connecting);
                            connectingToGatt = true;
                            listener.onRSSIUpdate(NotifyConnector.this, rssi);
                            device.connectGatt(context, true, NotifyConnector.this);
                            bluetoothAdapter.stopLeScan(this);
                        }
                    }
                }
            });
        } else {
            Log.e(TAG, "cannot connect, in state:" + connectionStateString(state));
        }
    }

    private void setCurrentState(ConnectionState state) {
        this.state = state;
        listener.onConnectionStateChanged(NotifyConnector.this, state);
    }

    public NotifyConnector(BluetoothAdapter adapter, Context context, UUID[] serviceUUIDs, UUID characteristic, UUID serviceUUID) {
        this.bluetoothAdapter = adapter;
        this.context = context;
        this.serviceUUIDs = serviceUUIDs;
        this.characteristic = characteristic;
        this.serviceUUID = serviceUUID;
    }


    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        Log.d(TAG, "onConnectionStateChange:" + gatt +
                " status:" + BluetoothUtil.statusToString(status) +
                " newState:" + BluetoothUtil.connectionStateToString(newState) +
                " NotifyConnector.state:" + connectionStateString(state));

        switch (newState) {
            case BluetoothGatt.STATE_CONNECTING: {
                if (state != ConnectionState.connecting) {
                    Log.e(TAG, "onConnectionStateChange connectingConfirmed  when state was not connecting: "+ connectionStateString(state));
                }
                setCurrentState(ConnectionState.connectingConfirmed);

                break;
            }
            case BluetoothGatt.STATE_CONNECTED: {
                if (state == ConnectionState.connectingConfirmed || state == ConnectionState.connecting) {
                    connectingToGatt = false;
                    gatt.discoverServices();
                    setCurrentState(ConnectionState.connecting);
                    this.connectedGatt = gatt;
                } else {
                    Log.e(TAG, "onConnectionStateChange when the NotifiyConnector was in state "+ connectionStateString(state));
                }
                break;
            }
            case BluetoothGatt.STATE_DISCONNECTING:
                setCurrentState(ConnectionState.disconnecting);
                break;
            case BluetoothGatt.STATE_DISCONNECTED: {
                setCurrentState(ConnectionState.disconnected);
                this.connectedGatt = null;
                break;
            }
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        gatt.readRemoteRssi();
    }


    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        BluetoothGattCharacteristic valueCharacteristic = gatt.getService(serviceUUID).getCharacteristic(characteristic);
        boolean notificationSet = gatt.setCharacteristicNotification(valueCharacteristic, true);

        BluetoothGattDescriptor descriptor = valueCharacteristic.getDescriptor(BTLE_NOTIFICATION_DESCRIPTOR_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        boolean writeDescriptorSuccess = gatt.writeDescriptor(descriptor);
    }


}
