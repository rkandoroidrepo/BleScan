package com.example.harman.ble.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.harman.ble.BLEPowerSensorManager;
import com.example.harman.ble.util.GattHeartRateAttributes;

import java.util.List;
import java.util.UUID;

import static com.example.harman.ble.BLEPowerSensorManager.CHARACTERISTIC_UUID_CYCLING_POWER;

/**
 * Created by rauliyohmc on 05/03/15.
 */

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device, as well as HTTP communication with a web server controlled by us
 */

public class BluetoothLeService extends Service {

    public final static String ACTION_GATT_CONNECTED =
            "com.rauliyohmc.heartratemonitor.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.rauliyohmc.heartratemonitor.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.rauliyohmc.heartratemonitor.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.rauliyohmc.heartratemonitor.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.rauliyohmc.heartratemonitor.EXTRA_DATA";

    public final static String ACTION_BIKE_POWER_AVAILABLE = "com.rauliyohmc.heartratemonitor.ACTION_BIKE_POWER_AVAILABLE";

    public final static UUID UUID_HEART_RATE_MEASUREMENT =
            UUID.fromString(GattHeartRateAttributes.UUID_HEART_RATE_MEASUREMENT);
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    // Binder to expose Service API to the Activity bound
    private final IBinder iBinder = new LocalBinder();
    // Bluetooth
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private String bluetoothDeviceAddress;
    private BluetoothGatt bluetoothGatt;
    //    private HttpConnection httpConnection;
    private int connectionState = STATE_DISCONNECTED;
    /**
     * Implements callback methods for GATT events that the app cares about.
     * For example, connection change and services discovered.
     */
    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                Log.d(TAG, "onConnectionStateChange. Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery:" + bluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Inform the web server to disconnect
                // httpConnection.sendHeartRateToWebServer(0);
                connectionState = STATE_DISCONNECTED;
                Log.d(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }

//           BluetoothGattService servicePower = gatt.getService(UUID.fromString(BLEPowerSensorManager.SERVICE_UUID_CYCLING_POWER));
//            if (null != servicePower) {
//                Log.i(TAG, "Power Service Discovered - Success， status = " + status);
//                BluetoothGattCharacteristic characteristicPower = servicePower.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_CYCLING_POWER));
//                if (null != characteristicPower) {
//                    gatt.setCharacteristicNotification(characteristicPower, true);
//                    BluetoothGattDescriptor firstDesc = characteristicPower.getDescriptor(BLEPowerSensorManager.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
//                    firstDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    gatt.writeDescriptor(firstDesc);
//                }
//            }

//            BluetoothGattService serviceSpeedAndCadence = gatt.getService(UUID.fromString(BLEPowerSensorManager.SERVICE_UUID_CYCLING_SPEED_AND_CADENCE));
//            if (null != serviceSpeedAndCadence) {
//                for (BluetoothGattCharacteristic characteristic : serviceSpeedAndCadence.getCharacteristics()) {
//                    Log.w(TAG, "#### CSC Sensor - Characteristic UUID : " + characteristic.getUuid().toString().toUpperCase());
//                }
//
//                BluetoothGattCharacteristic characteristicCSC = serviceSpeedAndCadence.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_CSC_MEASUREMENT));
//                if (characteristicCSC != null) {
//                    gatt.setCharacteristicNotification(characteristicCSC, true);
//                    BluetoothGattDescriptor firstDesc = characteristicCSC.getDescriptor(BLEPowerSensorManager.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
//                    firstDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
//                    gatt.writeDescriptor(firstDesc);
//                }
//            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "onCharacteristicRead() called");
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged() called. Heart rate value changed");


            if (characteristic.getUuid().toString().equalsIgnoreCase(CHARACTERISTIC_UUID_CYCLING_POWER)) {
                // Read power data
                int power = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
                Log.i(TAG, "Power Data： power = " + power + " W");

                // Bike speed
                // Read and calculate speed and cadence value
                int offset = 0;
                final int flag = characteristic.getValue()[offset];
                offset += 1;

                // Wheel Revolution Data Present, index 0, size 1 bit, 0 False, 1 True
                final boolean wheelRevPresent = (flag & 0x01) > 0;

                // Field exists if the key of bit 0 of the Flags field is set to 1
                int wheelRevolutions = 0;       // wheel revolutions count
                // Unit has a resolution of 1/1024s.
                // C1: Field exists if the key of bit 0 of the Flags field is set to 1.
                int lastWheelEventTime = 0;     // wheel data last capture time
                if (wheelRevPresent) {
                    wheelRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
                    offset += 4;

                    lastWheelEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    offset += 2;
                }

                // Crank Revolution Data Present, index 1, size 1 bit, 0 False, 1 True.
                final boolean crankRevPresent = (flag & 0x02) > 0;
                int crankRevolutions = 0;   // Crank revolution count
                int lastCrankEventTime = 0;
                if (crankRevPresent) {
                    crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    offset += 2;

                    lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                }

                System.out.println(TAG+"Speed-"+wheelRevolutions+"-lastWheelEventTime-"+lastWheelEventTime);
                System.out.println(TAG+"Cadence-"+crankRevolutions+"-lastCrankEventTime-"+lastCrankEventTime);


                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            } else {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);

            }

         /*   if (characteristic.getUuid().toString().equalsIgnoreCase(CHARACTERISTIC_UUID_CSC_MEASUREMENT)) {
                // Read and calculate speed and cadence value
                int offset = 0;
                final int flag = characteristic.getValue()[offset];
                offset += 1;

                // Wheel Revolution Data Present, index 0, size 1 bit, 0 False, 1 True
                final boolean wheelRevPresent = (flag & 0x01) > 0;

                // Field exists if the key of bit 0 of the Flags field is set to 1
                int wheelRevolutions = 0;       // wheel revolutions count
                // Unit has a resolution of 1/1024s.
                // C1: Field exists if the key of bit 0 of the Flags field is set to 1.
                int lastWheelEventTime = 0;     // wheel data last capture time
                if (wheelRevPresent) {
                    wheelRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, offset);
                    offset += 4;

                    lastWheelEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    offset += 2;
                }

                // Crank Revolution Data Present, index 1, size 1 bit, 0 False, 1 True.
                final boolean crankRevPresent = (flag & 0x02) > 0;
                int crankRevolutions = 0;   // Crank revolution count
                int lastCrankEventTime = 0;
                if (crankRevPresent) {
                    crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                    offset += 2;

                    lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
                }

                System.out.println(TAG+"Speed-"+wheelRevolutions+"-lastWheelEventTime-"+lastWheelEventTime);
                System.out.println(TAG+"Cadence-"+crankRevolutions+"-lastCrankEventTime-"+lastCrankEventTime);

               // mCallbacks.onSpeedMeasurementReceived(wheelRevolutions, lastWheelEventTime);
              //  mCallbacks.onCadenceMeasurementReceived(crankRevolutions, lastCrankEventTime);
            }*/

        }
    };
    private String currentLatitude;
    private String currentLongitude;

    // Creating an instance of httpConnectionClass to handle HTTP operations
    @Override
    public void onCreate() {
        super.onCreate();
        // httpConnection = new HttpConnection(HttpConnection.URL_HEART_RATE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind() called");
        return iBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        Log.d(TAG, "Releasing resources, onUnbind() called");
        close();
        return super.onUnbind(intent);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastBikePower(String action, String power) {
        final Intent intent = new Intent(action);
        intent.putExtra(EXTRA_DATA, power);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            int flag = characteristic.getProperties();
            int format = -1;
            if ((flag & 0x01) != 0) {
                format = BluetoothGattCharacteristic.FORMAT_UINT16;
                Log.d(TAG, "Heart rate format UINT16.");
            } else {
                format = BluetoothGattCharacteristic.FORMAT_UINT8;
                Log.d(TAG, "Heart rate format UINT8.");
            }
            final int heartRate = characteristic.getIntValue(format, 1);
            Log.d(TAG, String.format("Received heart rate: %d", heartRate));
            // We send the heartRate value to our Server throughout a HTTP post request
            //httpConnection.sendHeartRateToWebServer(heartRate);
            intent.putExtra(EXTRA_DATA, String.valueOf(heartRate));
        }
//        else if(CHARACTERISTIC_UUID_CYCLING_POWER.equalsIgnoreCase(characteristic.getUuid().toString())){
//            int power = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
//            intent.putExtra(EXTRA_DATA, String.valueOf(power));
//        }

        else {

            if (CHARACTERISTIC_UUID_CYCLING_POWER.equalsIgnoreCase(characteristic.getUuid().toString())) {
                int power = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
                intent.putExtra(EXTRA_DATA, String.valueOf(power));
            } else {


                // Inform the web server to disconnect
                // httpConnection.sendHeartRateToWebServer(0);
                // For all other profiles, writes the data formatted in HEX.

                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
            }

        }
        sendBroadcast(intent);
    }

    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        Log.d(TAG, "GATT client-server connection. Starting connection, connect() called");
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (bluetoothDeviceAddress != null && address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                connectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        Log.d(TAG, "GATT client-server connection. Device found, connection ongoing");
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.

        /**
         * Connect to GATT Server hosted by this device
         * Caller acts as GATT client (BluetoothLeService)
         * The callback is used to deliver results to Caller,
         * such as connection status as well as any further GATT client operations
         * The method returns a BluetoothGatt instance
         * You can use BluetoothGatt to conduct GATT client operations.
         */
        bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        Log.d(TAG, "Releasing Resources. close() called");
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
//        httpConnection.disconnect();
//        httpConnection = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        // This is specific to Heart Rate Measurement.
        // We access the descriptor 'Client Characteristic Configuration' to set the notification flag to 'enabled'
        // Thereby, HEART_RATE_MEASUREMENT characteristic is able to send notifications, whenever the data underlying changes
        if (UUID_HEART_RATE_MEASUREMENT.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    UUID.fromString(GattHeartRateAttributes.UUID_CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    public BluetoothGatt getBluetoothGatt() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt;
    }

    public void getBikePower(BluetoothGattCharacteristic characteristicPower) {
        if (null != characteristicPower) {
            bluetoothGatt.setCharacteristicNotification(characteristicPower, true);
            BluetoothGattDescriptor firstDesc = characteristicPower.getDescriptor(BLEPowerSensorManager.CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
            firstDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(firstDesc);
        }
    }

    String speedService  = "d445fe01-d139-9a5d-6707-1cc6a58b6303";
    String charec = "d445fe02-d139-9a5d-6707-1cc6a58b6303";
    String desc = "00002902-0000-1000-8000-00805f9b34fb";
    public void getBikeSpeed(BluetoothGattCharacteristic characteristic){
        if(characteristic!=null){
            bluetoothGatt.setCharacteristicNotification(characteristic, true);
            BluetoothGattDescriptor firstDesc = characteristic.getDescriptor(UUID.fromString(desc));
            firstDesc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(firstDesc);
        }
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
}
