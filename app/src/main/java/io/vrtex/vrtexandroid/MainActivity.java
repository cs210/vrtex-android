package io.vrtex.vrtexandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
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

    private String ipAddr;
    private int port;
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
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
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
        ipAddr = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("ip_address", getResources().getString(R.string.pref_default_ip_address));
        port = Integer.parseInt(PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("ip_port", getResources().getString(R.string.pref_default_ip_port)));
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW)
            return;

        float[] mRotationMatrixFromVector = new float[16];
        float[] mRotationMatrix = new float[16];
        float[] orientationVals = new float[4];

        String value0 = Float.toString(event.values[0]);
        String value1 = Float.toString(event.values[1]);
        String value2 = Float.toString(event.values[2]);

        // Convert the rotation-vector to a 4x4 matrix.
        SensorManager.getRotationMatrixFromVector(mRotationMatrixFromVector, event.values);
        SensorManager.remapCoordinateSystem(
                mRotationMatrixFromVector,
                //SensorManager.AXIS_Y,
                //SensorManager.AXIS_MINUS_X,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Y,
                mRotationMatrix);
        SensorManager.getOrientation(mRotationMatrix, orientationVals);
        /*
        SensorManager.getOrientation(mRotationMatrixFromVector, orientationVals);
        */

        // Optionally convert the result from radians to degrees
        /*
        orientationVals[0] = (float) Math.toDegrees(orientationVals[0]);
        orientationVals[1] = (float) Math.toDegrees(orientationVals[1]);
        orientationVals[2] = (float) Math.toDegrees(orientationVals[2]);
        float denom = (float) (Math.PI / 2);
         */

        float yaw = orientationVals[0];
        float roll = -orientationVals[2];
        float pitch = orientationVals[1];

        textView.setText(
                " Yaw: " + yaw +
                        "\n Pitch: " + pitch +
                        "\n Roll:  " + roll
        );

        List args = new ArrayList();
        args.add(yaw);
        args.add(pitch);
        args.add(roll);
        args.add(0);
        sendOSC("/accxyz", args);
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
        OSCMessage msg = new OSCMessage(address, arguments);
        new AsyncSendOSCTask(this, this.oscPortOut).execute(msg);
    }
}
