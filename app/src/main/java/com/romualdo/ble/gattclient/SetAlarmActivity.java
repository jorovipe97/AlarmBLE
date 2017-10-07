package com.romualdo.ble.gattclient;

import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Calendar;

public class SetAlarmActivity extends AppCompatActivity implements
        SelectTimeFragment.OnFragmentInteractionListener,
        SelectDateFragment.OnFragmentInteractionListener {

    private TextView textClock;
    private TextView textDate;
    private Calendar alarmOnDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);

        textClock = (TextView) findViewById(R.id.textClock);
        textDate = (TextView) findViewById(R.id.textDate);

        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minut = c.get(Calendar.MINUTE);

        textClock.setText(formatTime(hour) + ":" + formatTime(minut));
        textDate.setText("Dia en el que sonara la alarma");
    }

    public void showTimePickerDialog(View view) {
        DialogFragment fragment = new SelectTimeFragment();
        fragment.show(getSupportFragmentManager(), "TimePicker");
    }

    public void showDatePickerDialog(View view) {
        DialogFragment fragment = new SelectDateFragment();
        fragment.show(getSupportFragmentManager(), "DatePicker");
    }

    private String formatTime(int hourOrMinuts) {
        String str = hourOrMinuts+"";
        if (hourOrMinuts < 9) {
            str = "0" + hourOrMinuts;
        }

        return str;
    }

    public void onPickerTimeSet(int hour, int minuts) {
        textClock.setText("");
        textClock.setText(formatTime(hour) + ":" + formatTime(minuts));
    }

    public void onPickerDateSet(Calendar c) {

        textDate.setText(c.get(Calendar.DAY_OF_MONTH) + "/" + (c.get(Calendar.MONTH)+1) + "/" + c.get(Calendar.YEAR));

    }
}
