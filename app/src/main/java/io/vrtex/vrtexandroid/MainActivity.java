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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;
import com.illposed.osc.OSCPortIn;

import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;


public class MainActivity extends ActionBarActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private TextView textView;
    private Button useBeamButton;
    private Button beamButton;
    private Button shieldButton;
    private Button parkButton;
    private Button driveButton;

    private String ipAddr;
    private int port;
    private OSCPortOut oscPortOut = null;
    private int inPort = 8090;
    private OSCPortIn oscPortIn = null;

    private OSCListener oscListener;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        textView = (TextView)findViewById(R.id.dataOutput);
        useBeamButton = (Button)findViewById(R.id.useBeamButton);
        beamButton = (Button)findViewById(R.id.beamButton);
        shieldButton = (Button)findViewById(R.id.shieldButton);
        parkButton = (Button)findViewById(R.id.parkButton);
        driveButton = (Button)findViewById(R.id.driveButton);
        random = new Random();

        useBeamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOSC("/usebeam", null);
            }
        });

        beamButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer randomInt = randomFingerCount();
                textView.setText(randomInt.toString() + " fingers");
                disableButtons();
                beamButton.setText("Activating...");
                sendOSC("/modebeam", randomInt);
            }
        });

        shieldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer randomInt = randomFingerCount();
                textView.setText(randomInt.toString() + " fingers");
                disableButtons();
                shieldButton.setText("Activating...");
                sendOSC("/modeshield", randomInt);
            }
        });

        parkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer randomInt = randomFingerCount();
                textView.setText(randomInt.toString() + " fingers");
                disableButtons();
                parkButton.setText("Shifting...");
                sendOSC("/modepark", randomInt);
            }
        });

        driveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Integer randomInt = randomFingerCount();
                textView.setText(randomInt.toString() + " fingers");
                disableButtons();
                driveButton.setText("Shifting...");
                sendOSC("/modedrive", randomInt);
            }
        });

        final Context _this = this;

        this.oscListener = new OSCListener() {
            @Override
            public void acceptMessage(Date date, OSCMessage oscMessage) {
                String address = oscMessage.getAddress();
                final List<Object> args = oscMessage.getArguments();
                switch(address) {
                    case "/android/state":
                        final boolean isParked = ((Integer)args.get(0)) == 1;
                        final boolean isBeamActive = ((Integer)args.get(1)) == 1;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beamButton.setEnabled(!isBeamActive);
                                shieldButton.setEnabled(isBeamActive);
                                parkButton.setEnabled(!isParked);
                                driveButton.setEnabled(isParked);
                            }
                        });
                        break;
                    case "/android/text":
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText((String)args.get(0));
                            }
                        });
                        break;

                    case "/android/becomeBeam":
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                shieldButton.setEnabled(true);
                                beamButton.setEnabled(false);
                                beamButton.setText(R.string.beam);
                                useBeamButton.setVisibility(View.VISIBLE);
                                textView.setText(R.string.awaiting_command);
                            }
                        });
                        break;
                    case "/android/becomeShield":
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beamButton.setEnabled(true);
                                shieldButton.setEnabled(false);
                                shieldButton.setText(R.string.shield);
                                useBeamButton.setVisibility(View.INVISIBLE);
                                textView.setText(R.string.awaiting_command);
                            }
                        });
                        break;
                    case "/android/becomePark":
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                driveButton.setEnabled(true);
                                beamButton.setEnabled(true);
                                parkButton.setEnabled(false);
                                parkButton.setText(R.string.park);
                                textView.setText(R.string.awaiting_command);
                            }
                        });
                        break;
                    case "/android/becomeDrive":
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                beamButton.setEnabled(false);
                                driveButton.setEnabled(false);
                                parkButton.setEnabled(true);
                                driveButton.setText(R.string.drive);
                                textView.setText(R.string.awaiting_command);
                            }
                        });
                        break;
                    case "/android/notInPark":
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                textView.setText(R.string.not_in_park);
                            }
                        });
                        break;
                    default:
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(_this, "Invalid OSC Message", Toast.LENGTH_SHORT);
                            }
                        });
                        break;
                }
            }
        };
    }

    private void enableButtons() {
        beamButton.setEnabled(true);
        shieldButton.setEnabled(true);
        parkButton.setEnabled(true);
        driveButton.setEnabled(true);
    }

    private void disableButtons() {
        beamButton.setEnabled(false);
        shieldButton.setEnabled(false);
        parkButton.setEnabled(false);
        driveButton.setEnabled(false);
    }

    private int randomFingerCount() {
        return random.nextInt(4) + 2;
    }

    private View.OnClickListener handleClick = new View.OnClickListener(){
        public void onClick(View view) {
            String test = view.getResources().getResourceName(view.getId());
            sendOSC("/" + test + "Pressed", 1);
        }
    };

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
        initializeIncomingOSC();
        initializeState();
    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);

        if(this.oscPortOut != null) {
            this.oscPortOut.close();
        }

        if (this.oscPortIn != null) {
            if (this.oscPortIn.isListening()) {
                this.oscPortIn.stopListening();
            }
            this.oscPortIn.close();
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

        /*textView.setText(
                " Yaw: " + yaw +
                        "\n Pitch: " + pitch +
                        "\n Roll:  " + roll
        );*/

        List args = new ArrayList();
        args.add(yaw);
        args.add(pitch);
        args.add(roll);
        args.add(0);
        sendOSC("/accxyz", args);
    }

    private void initializeState() {
        sendOSC("/android/state", null);
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

    private void initializeIncomingOSC() {
        if (this.oscPortIn != null) {
            if (this.oscPortIn.isListening()) {
                this.oscPortIn.stopListening();
            }

            this.oscPortIn.close();
            this.oscPortIn = null;
        }

        try {
            this.oscPortIn = new OSCPortIn(this.inPort);
            this.oscPortIn.addListener("/android/*", oscListener);
            this.oscPortIn.startListening();
        }
        catch(SocketException se) {
            se.printStackTrace();
            Log.e("MainActivity", se.getMessage());
        }
    }

    public void sendOSC(String address, Object argument) {
        List<Object> args = new ArrayList<Object>();
        if (argument.getClass().equals(Boolean.class))
            args.add((Boolean)argument ? 1 : 0);
        else
            args.add(argument);
        sendOSC(address, args);
    }

    public void sendOSC(String address, List<Object> arguments) {
        OSCMessage msg = new OSCMessage(address, arguments);
        new AsyncSendOSCTask(this, this.oscPortOut).execute(msg);
    }
}
