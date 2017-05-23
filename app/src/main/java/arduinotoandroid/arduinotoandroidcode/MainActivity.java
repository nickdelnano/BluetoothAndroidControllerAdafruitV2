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

    int scale = 17;
    int numberOfNotches = 255 / scale;
    //adafruit dc motors take speed inputs between -255 < 0 < 255

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setListeners();
        CheckBtIsOn();
    }

    @Override
    public void onClick(View control) {
        String id = deviceId.getText().toString();

        // Only make BT connection if device id was entered
        if (!id.equals("")) {
        }

        else {

            switch (control.getId()) {
                case R.id.connect:
                    connect(id);
                    connectedStatus.setText(R.string.connection_connected);
                    break;
                case R.id.stopMotors: {
                    writeData("stop x");
                }
            }
        }
    }

    private void CheckBtIsOn() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "Bluetooth Disabled!",
                    Toast.LENGTH_SHORT).show();

            //start activity to request bluetooth to be enabled
            Intent enableBtIntent = new Intent(mBluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void connect(String id) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(id);

        mBluetoothAdapter.cancelDiscovery();

        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            btSocket.connect();
            Log.d(TAG, "Connection made.");
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                Log.d(TAG, "Unable to end the connection");
            }
            Log.d(TAG, "Socket creation failed");
        }
    }

    private void writeData(String data) {
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Log.d(TAG, "Bug BEFORE Sending stuff", e);
        }

        String message = data;
        byte[] msgBuffer = message.getBytes();

        try {
            outStream.write(msgBuffer);
        } catch (IOException e) {
            Log.d(TAG, "Bug while sending stuff", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            btSocket.close();
        } catch (IOException e) {
        }
    }

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
                //convert
                progress = progress - numberOfNotches;
                progress = progress * scale;

                //direction is sent as 0 for forward or 1 for backward
                //default is forward
                leftDirection = 0;

                //if progress is negative, set direction to 1 and then take abs to avoid sending "-" over bluetooth
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
                //convert
                progress = progress - numberOfNotches;
                progress = progress * scale;

                //direction is sent as 0 for forward or 1 for backward
                //default is forward
                rightDirection = 0;

                //if progress is negative, set direction to 1 and then take abs to avoid sending "-" over bluetooth
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