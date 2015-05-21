package io.vrtex.vrtexandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class MainActivity extends ActionBarActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private TextView textView;

    private String ipAddr = "127.0.0.1";
    private int port = 8000;
    private OSCPortOut oscPortOut = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        textView = (TextView)findViewById(R.id.dataOutput);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id){
            case R.id.action_settings:
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        // restoreNetworkSettingsFromFile();
        initializeOSC();
        // initializeIncomingOSC();
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

        if(this.oscPortOut != null) {
            this.oscPortOut.close();
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        String value0 = Float.toString(event.values[0]);
        String value1 = Float.toString(event.values[1]);
        String value2 = Float.toString(event.values[2]);
        textView.setText(value0 + ", " + value1 + ", " + value2);

        List args = new ArrayList();
        for (Float f : event.values) {
            args.add(f);
        }
        //sendOSC(ipAddr, args);
    }

    /***
     * Initializes the OSCPortOut class with the given ipAddress and port.
     * Called once at the beginning in onCreate() method and at the end of the network settings dialog save action.
     */
    private void initializeOSC() {
        try {

            if(oscPortOut != null) {
                oscPortOut.close();
            }

            oscPortOut = new OSCPortOut(InetAddress.getByName(ipAddr), port);
        }
        catch(Exception exp) {
            Toast.makeText(this, exp.toString(), Toast.LENGTH_LONG).show();
            oscPortOut = null;
        }
    }

    public void sendOSC(String address, List<Object> arguments) {
        try {
            this.oscPortOut.send(new OSCMessage(address, arguments));
        }
        catch(Exception exp) {
            Toast.makeText(this, "Error Sending Message", Toast.LENGTH_SHORT).show();
        }
    }
}
