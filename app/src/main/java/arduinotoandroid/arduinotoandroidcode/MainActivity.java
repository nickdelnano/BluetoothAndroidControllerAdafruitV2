package arduinotoandroid.arduinotoandroidcode;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * This Activity acts as a server for a bluetooth client and implements a text-based protocol
 * to control two sets of two bidirectional motors. An arduino client controlling motors using
 * the Adafruit Motor Shield V2 can be found at https://github.com/ndelnano/ArduinoBluetooth/blob/master/bluetooth.ino.
 *
 * The text based protocol is as follows:
 *      'stop x' - stop all motors
 *      'move {LEFT_DIRECTION}{LEFT_SPEED} {RIGHT_DIRECTION}{RIGHT_SPEED} x'
 *          - Where *_DIRECTION are 0 or 1, and *_SPEED is in range [-255,255]
 *              *_DIRECTION = 0 => forward, 1 => backwards
 *
 *
 *  Ex: "move 0100 0100x" --sets both wheels forward at speed 100
 *      "move 0255 1255x" --sets left wheel at max speed forward, right wheel at max speed backward
 *              -- This will make your vehicle do an awesome donut!
 */
public class MainActivity extends Activity implements OnClickListener {

    private static final String TAG = "ArduinoToAndroid";
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final static int REQUEST_ENABLE_BT = 1;

    @Bind(R.id.verticalSeekbarLeft)
    VerticalSeekBar mVerticalSeekbarLeft;
    @Bind(R.id.verticalSeekbarRight)
    VerticalSeekBar mVerticalSeekbarRight;

    @Bind(R.id.deviceId)
    EditText deviceId;

    @Bind(R.id.seekbarLeftNumber)
    TextView mLeftSliderText;
    @Bind(R.id.seekbarRightNumber)
    TextView mRightSliderText;

    @Bind(R.id.connect)
    Button connect;
    @Bind(R.id.stopMotors)
    Button stopMotors;

    @Bind(R.id.connectedStatus)
    TextView connectedStatus;

    private SpeedHolder speedHolder = new SpeedHolder();
    int rightDirection;
    int leftDirection;

    // Create a scale for the vertical sliders
    // Since the Adafruit Motor Shield takes inputs between -255 < 0 < 255,
    // Create a scale that corresponds to the number of distinct values returned by the VerticalSeekBar
    // in activity_main.xml
    // This functionality does not modify the values sent over the network, but ensures that
    // only valid values in the range [-255,255] can be sent.
    int scale = 17;
    int numberOfNotches = 255 / scale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views to activity instance variables
        ButterKnife.bind(this);

        // Set button listeners
        setListeners();

        // If bluetooth is not turned on, prompt user to turn it on
        CheckBtIsOn();
    }

    /**
     * Onclick functions for connect and stop motors buttons.
     * @param control
     */
    @Override
    public void onClick(View control) {
        switch (control.getId()) {

            // Initiate bluetooth connection
            case R.id.connect:
                // Only make BT connection if device id was entered
                String id = deviceId.getText().toString();
                if (id.equals("")) {
                    Toast.makeText(getApplicationContext(), "Enter the bluetooth receiver ID into the text field.",
                            Toast.LENGTH_SHORT).show();
                }
                else {
                    connect(id);
                }
                break;

                // Stop the motors
                case R.id.stopMotors: {
                    // Send stop message over bluetooth
                    writeData("stop x");
                    break;
                }
            }
    }

    /**
     * If bluetooth is not enabled on the users device, prompt them with a menu to enable it.
     */
    private void CheckBtIsOn() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth Disabled!",
                    Toast.LENGTH_SHORT).show();

            // Start activity to show bluetooth options
            Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Connect to the bluetooth device with id 'id'. Id must be a valid bluetooth address
     * in the form "00:11:22:33:AA:BB".
     * @param id
     */
    public void connect(String id) {
        // Android bluetooth API code
        BluetoothDevice device = null;
        try {
            device = mBluetoothAdapter.getRemoteDevice(id);
            mBluetoothAdapter.cancelDiscovery();
        }
        catch (Exception e) {
            // Show user error message
            // i.e. "333 is not a valid bluetooth address"
            Toast.makeText(getApplicationContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }

        // If getRemoteDevice did not throw and device was set, connect to client
        // getRemoteDevice throws if its parameter is not a valid bluetooth address
        if (device != null) {
            try {
                btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                Toast.makeText(getApplicationContext(), "Connection made",
                        Toast.LENGTH_SHORT).show();
                connectedStatus.setText(R.string.connection_connected);
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.d(TAG, "Unable to end the connection");
                }
                Log.d(TAG, "Socket creation failed");
            }
        }
    }

    /**
     * Writes the string 'data' over the global bluetooth socket 'btSocket'
     * @param data
     */
    private void writeData(String data) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
        }
    }

    /**
     * Closes the global bluetooth socket 'btSocket'
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            // Close bluetooth socket
            btSocket.close();
        } catch (IOException e) {
        }
    }

    /**
     * Set listeners for buttons -- 'connect', 'stop motors', left scrollbar, right scrollbar
     * Implements text based protocol described at top of file
     */
    public void setListeners()
    {
        connect.setOnClickListener(this);
        stopMotors.setOnClickListener(this);

        mVerticalSeekbarLeft.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert using scale
                progress = progress - numberOfNotches;
                progress = progress * scale;

                // Direction is sent as 0 for forward or 1 for backward
                leftDirection = 0;

                // If progress is negative, set direction to 1 and then take abs to avoid sending "-" over bluetooth
                if (progress < 0) {
                    leftDirection = 1;
                    progress = Math.abs(progress);
                }

                speedHolder.setLeftSpeed("" + leftDirection + progress);
                mLeftSliderText.setText(String.valueOf(progress));

                writeData("move " + speedHolder.getLeftSpeed() + " " + speedHolder.getRightSpeed() + "x");
            }

        });

        mVerticalSeekbarRight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Convert using scale
                progress = progress - numberOfNotches;
                progress = progress * scale;

                // Direction is sent as 0 for forward or 1 for backward
                rightDirection = 0;

                // If progress is negative, set direction to 1 and then take abs to avoid sending "-" over bluetooth
                if (progress < 0) {
                    rightDirection = 1;
                    progress = Math.abs(progress);
                }

                speedHolder.setRightSpeed("" + rightDirection + progress);
                mRightSliderText.setText(String.valueOf(progress));

                writeData("move " + speedHolder.getLeftSpeed() + " " + speedHolder.getRightSpeed() + "x");
            }
        });
    }
}