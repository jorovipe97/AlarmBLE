package com.romualdo.ble.gattclient;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SetAlarmActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_alarm);
    }

    public void showTimePickerDialog(View view) {
        DialogFragment fragment = new SelectTimeFragment();
        fragment.show(getSupportFragmentManager(), "TimePicker");
    }

    public void showDatePickerDialog(View view) {
        DialogFragment fragment = new SelectDateFragment();
        fragment.show(getSupportFragmentManager(), "DatePicker");
    }
}
