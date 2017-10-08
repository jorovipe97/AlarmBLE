package com.romualdo.ble.gattclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.romualdo.ble.common.WakeLocker;

public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //hrow new UnsupportedOperationException("Not yet implemented");

        //Toast.makeText(context, "Alarm receiver called", Toast.LENGTH_LONG).show();
        WakeLocker.acquire(context.getApplicationContext());
        Intent i = new Intent(context.getApplicationContext(), MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(MainActivity.EXTRA_IS_FROM_ALARM, true);
        context.startActivity(i);

    }
}
