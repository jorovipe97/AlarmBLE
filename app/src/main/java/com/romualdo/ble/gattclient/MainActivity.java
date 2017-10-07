package com.romualdo.ble.gattclient;

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
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.romualdo.ble.common.Ints;

import org.w3c.dom.Text;

import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRA_INPUTVAL = "com.romualdo.ble.blink.extra_inputval";

    public static final String MAC_ADDRESS = "CA:A5:4F:3A:A9:5C";
    public static final UUID UUID_SERVICE = UUID.fromString("0000fe84-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISTIC_BUTTONSTATUS = UUID.fromString("2d30c082-f39f-4ce6-923f-3484ea480596");
    public static final UUID UUID_CHARACTERISTIC_LED = UUID.fromString("2d30c083-f39f-4ce6-923f-3484ea480596");

    /**
     * Services, characteristics, and descriptors are collectively
     * referred to as attributes and identified by UUIDs (128 bit number).
     * Of those 128 bits, you typically only care about the 16 bits highlighted
     * below. These digits are predefined by the Bluetooth Special Interest Group
     * (SIG).

     xxxxXXXX-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */

    /*
    Read and write descriptors for a particular characteristic.
    One of the most common descriptors used is the Client Characteristic
    Configuration Descriptor. This allows the client to set the notifications
    to indicate or notify for a particular characteristic. If the client sets
    the Notification Enabled bit, the server sends a value to the client whenever
    the information becomes available. Similarly, setting the Indications Enabled
    bit will also enable the server to send notifications when data is available,
    but the indicate mode also requires a response from the client.

    Source: https://goo.gl/EaK6au

     */

    // This is one of the most used descriptor: Client Characteristic Configuration Descriptor. 0x2902
    public static final UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 1;

    private Context mContext;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private Button connectBtn;
    private Button disconectBtn;
    private TextView statusBtn;
    private Button btnOnOff;
    private boolean ledStatus = false;
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        private final String TAG = "mGattCallback";

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            super.onConnectionStateChange(gatt, status, newState);
            Log.i(TAG, status + " " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectBtn.setEnabled(false);
                        disconectBtn.setEnabled(true);
                    }
                });
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        connectBtn.setEnabled(true);
                        disconectBtn.setEnabled(false);
                    }
                });
            }
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //Log.i(TAG, "Service discovered");

            if (status == gatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID_SERVICE);
                if (service != null) {
                    Log.i(TAG, "Service connected");
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID_CHARACTERISTIC_BUTTONSTATUS);
                    if (characteristic != null) {
                        Log.i(TAG, "Characteristic connected");
                        gatt.setCharacteristicNotification(characteristic, true);
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR);
                        if (descriptor != null) {
                            // Los descriptors son muy importntes
                            // TODO: Continue studying about descriptors in BLE
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                            Log.i(TAG, "Descriptor sended");
                        }
                    }

                    BluetoothGattCharacteristic characteristicLed = service.getCharacteristic(UUID_CHARACTERISTIC_LED);
                    if (characteristicLed != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnOnOff.setEnabled(true);
                            }
                        });
                    }
                }
            }
        }
/*
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
        }*/

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            readBtnStateCharacteristic(characteristic);
        }

        private void readBtnStateCharacteristic(BluetoothGattCharacteristic characteristic) {
            if (UUID_CHARACTERISTIC_BUTTONSTATUS.equals(characteristic.getUuid())) {
                byte[] data = characteristic.getValue();
                //int state = Ints.fromByteArray(data);
                Log.i(TAG, data[0] + "");
                if (data[0] == 1) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusBtn.setText("Button Down");
                        }
                    });
                } else if (data[0] == 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            statusBtn.setText("Button Up");
                        }
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);

        connectBtn = (Button) findViewById(R.id.buttonConnect);
        disconectBtn = (Button) findViewById(R.id.buttonDisconnect);
        statusBtn = (TextView) findViewById(R.id.btnStatus);
        btnOnOff = (Button) findViewById(R.id.btnOnOff);
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                turnOnOffLed();
            }
        });
        btnOnOff.setEnabled(false);

        // When the app is opened not show buttons
        connectBtn.setVisibility(View.INVISIBLE);
        disconectBtn.setVisibility(View.INVISIBLE);

        // Initializes Bluetooth adapter.
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            connectBtn.setVisibility(View.VISIBLE);
            disconectBtn.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopClient();
        ledStatus = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        // just UI topics
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.w(TAG, "Bluetooth enabled");
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                connectBtn.setVisibility(View.VISIBLE);
                disconectBtn.setVisibility(View.VISIBLE);
            }
            else {
                // Si no se pudo conectar
                connectBtn.setVisibility(View.INVISIBLE);
                disconectBtn.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "Bluetooth not enabled, closing app...", Toast.LENGTH_SHORT).show();
                // TODO: Close the app if bluetooth not enabled by user
            }
        }
    }

    private void turnOnOffLed() {
        ledStatus = !ledStatus;
        BluetoothGattCharacteristic ledCharacteristic = mBluetoothGatt
                .getService(UUID_SERVICE)
                .getCharacteristic(UUID_CHARACTERISTIC_LED);
        if (ledCharacteristic == null) {
            Toast.makeText(this, "Could not Get led characteristic", Toast.LENGTH_SHORT).show();
            return;
        }
        byte[] val = new byte[1];

        if (ledStatus) {
            val[0] = (byte) 1;
            Log.i(TAG, "Led status ON");
        } else {
            val[0] = (byte) 0;
        }
        ledCharacteristic.setValue(val);
        mBluetoothGatt.writeCharacteristic(ledCharacteristic);
    }

    // This is called when event onClick is fired
    public void startClient(View view) {
        try {
            BluetoothDevice bluetoothDevice = mBluetoothAdapter.getRemoteDevice(MAC_ADDRESS);
            mBluetoothGatt = bluetoothDevice.connectGatt(this, false, mGattCallback);
            Toast.makeText(this, "Connected to " + MAC_ADDRESS, Toast.LENGTH_SHORT).show();

            if (mBluetoothGatt == null) {
                Log.w(TAG, "Unable to create GATT client");
                Toast.makeText(this, "Cant connect to " + MAC_ADDRESS, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        catch (Exception e) {
            Log.w(TAG, e.toString());
        }

    }

    // Called when onClik event of disconect button is fired
    public void disconnect(View view) {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.disconnect();
        }
    }

    // Called when onDestroy event is fired
    // TODO: Call this in onDestroy event of current Activity
    public void stopClient() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter = null;
        }
    }

    public void openSetAlarmActivity(View view) {
        Intent intent = new Intent(this, SetAlarmActivity.class);
        startActivity(intent);
    }

}
