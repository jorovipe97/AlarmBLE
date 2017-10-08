package com.romualdo.ble.gattclient;

import android.app.AlarmManager;
import android.app.PendingIntent;
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
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.UUID;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity  implements
        SelectTimeFragment.OnFragmentInteractionListener,
        SelectDateFragment.OnFragmentInteractionListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String EXTRA_INPUTVAL = "com.romualdo.ble.blink.extra_inputval";
    public static final String EXTRA_IS_FROM_ALARM = "com.romualdo.ble.blink.isFromAlarm";

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
    public static final int REQUEST_SET_ALARM = 2;

    private Context mContext;
    public static BluetoothManager mBluetoothManager;
    public static BluetoothAdapter mBluetoothAdapter;
    public static BluetoothGatt mBluetoothGatt;
    private Button connectBtn;
    private Button disconectBtn;
    private TextView statusBtn;
    private Button btnOff;

    private TextView textClock;
    private TextView textDate;
    private Calendar alarmOnDate;

    private TextView timeCalendar;
    private TextView timeSystem;
    private Button btnSetAlarm;

    private static final String PREFERENCES_NAME = "MyPrefsFile";
    private SharedPreferences sharedPreferences;

    private AlarmManager alarmManager;

    private static boolean isAlarmFired = false;

    private boolean isAlarmSetted = false;
    private boolean canSetAlarm = false;
    private boolean isTimeSeted = false;
    private boolean isDateSeted = false;

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

                        Runnable myRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (isAlarmFired) {
                                    writeLedCharacteristic(true);
                                }
                            }
                        };
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.postDelayed(myRunnable, 1000);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnOff.setEnabled(true);
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
        btnOff = (Button) findViewById(R.id.btnOff);
        timeCalendar = (TextView) findViewById(R.id.currenTime1);
        timeSystem = (TextView) findViewById(R.id.currenTime2);

        btnSetAlarm = (Button) findViewById(R.id.btnSetAlarm);

        textClock = (TextView) findViewById(R.id.textClock);
        textDate = (TextView) findViewById(R.id.textDate);

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //turnOnOffAlarm();
                writeLedCharacteristic(false);
            }
        });
        btnOff.setEnabled(false);

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

        // TODO: Implement persistence in variable status: isAlarmSetted
        sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);

        alarmManager = (AlarmManager) getSystemService(this.ALARM_SERVICE);

        alarmOnDate = Calendar.getInstance(Locale.getDefault());



        startClient();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mBluetoothGatt.discoverServices();

        Intent intent = getIntent();
        boolean val = intent.getBooleanExtra(EXTRA_IS_FROM_ALARM, false);
        isAlarmFired = val;

        if (val) {
            Toast.makeText(this, "Called from alarm HDP", Toast.LENGTH_LONG).show();
            // Si minimizo y vuelvo a abrir la app, no se vuelve a ejecuutar este codigo
            // gracias a la siguiente linea
            intent.putExtra(EXTRA_IS_FROM_ALARM, false);

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
                connectBtn.setVisibility(View.VISIBLE);
                disconectBtn.setVisibility(View.VISIBLE);
                connectBtn.setEnabled(false);
                disconectBtn.setEnabled(false);
                Toast.makeText(this, "Bluetooth not enabled, closing app...", Toast.LENGTH_SHORT).show();
                // TODO: Close the app if bluetooth not enabled by user
            }
        } else if (requestCode == REQUEST_SET_ALARM) {
            Toast.makeText(this, "Hijueputa la alarma se llamo", Toast.LENGTH_LONG).show();
        }
    }

    // TODO: Reimplement this method functionality for only turn off the alarm.
    private void turnOnOffAlarm() {
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

    private boolean writeLedCharacteristic(boolean data) {
        BluetoothGattService ledService = mBluetoothGatt.getService(UUID_SERVICE);
        if (ledService == null) {
            Toast.makeText(this, "Could not Get led service", Toast.LENGTH_SHORT).show();
            return false;
        }
        BluetoothGattCharacteristic ledCharacteristic = ledService.getCharacteristic(UUID_CHARACTERISTIC_LED);
        if (ledCharacteristic == null) {
            Toast.makeText(this, "Could not Get led characteristic", Toast.LENGTH_SHORT).show();
            return false;
        }

        byte[] val = new byte[1];

        if (data) {
            val[0] = (byte) 1;
            //Log.i(TAG, "Led status ON");
        } else {
            val[0] = (byte) 0;
        }

        ledCharacteristic.setValue(val);
        mBluetoothGatt.writeCharacteristic(ledCharacteristic);
        Toast.makeText(this, "Written in led service, val = " + val[0], Toast.LENGTH_SHORT).show();
        return true;
    }

    public void startClient() {
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

    // This is called when event onClick is fired
    public void startClient(View view) {
        startClient();
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

    public void setAlarm(View view) {
        Toast.makeText(this, "Setting alarm", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_IS_FROM_ALARM, true);


        timeCalendar.setText(((alarmOnDate.getTimeInMillis()-System.currentTimeMillis())/1000)+"");
        timeSystem.setText(System.currentTimeMillis()+"");

        PendingIntent pendingIntent = PendingIntent.getActivity(this, REQUEST_SET_ALARM, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        //alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000*10, pendingIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, alarmOnDate.getTimeInMillis(), pendingIntent);

    }

    public void showTimePickerDialog(View view) {
        DialogFragment fragment = new SelectTimeFragment();
        fragment.show(getSupportFragmentManager(), "TimePicker");
    }

    public void showDatePickerDialog(View view) {
        DialogFragment fragment = new SelectDateFragment();
        fragment.show(getSupportFragmentManager(), "DatePicker");
    }


    public void onPickerTimeSet(int hour, int minuts) {
        isTimeSeted = true;
        if (isDateSeted && isTimeSeted) {
            canSetAlarm = true;
        }

        btnSetAlarm.setEnabled(canSetAlarm);


        String strhour = hour+"";
        String strminute = minuts+"";

        if (hour < 9) {
            strhour = "0" + hour;
        }
        if (minuts < 9) {
            strminute = "0" + minuts;
        }

        alarmOnDate.set(Calendar.HOUR_OF_DAY, hour);
        alarmOnDate.set(Calendar.MINUTE, minuts);
        textClock.setText("");
        /*Date time = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("H:m");*/
        textClock.setText(strhour + ":" + strminute);
    }

    public void onPickerDateSet(Calendar c) {
        isDateSeted = true;
        if (isDateSeted && isTimeSeted) {
            canSetAlarm = true;
        }
        btnSetAlarm.setEnabled(canSetAlarm);

        alarmOnDate.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DATE));
        textDate.setText(c.get(Calendar.DATE) + "/" + (c.get(Calendar.MONTH)+1) + "/" + c.get(Calendar.YEAR));
    }

}
