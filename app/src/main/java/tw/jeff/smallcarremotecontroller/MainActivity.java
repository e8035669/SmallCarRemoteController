package tw.jeff.smallcarremotecontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private final int REQUEST_BT = 1;           //onActivityResult
    private final int REQUEST_DEVICE_LIST = 2;
    private Handler bluetoothThreadHandler;
    // Note: Bluetooth
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothConnectThread bluetoothConnectThread = null;
    private boolean isBluetoothOpenByMe = false;

    // Note: Sensors
    private SensorManager sensorManager = null;
    private SensorEventListener sensorEventListener = null;
    private String sensorSendData = "";

    // Note:　Buttons
    private int btnStatus = 0b0000;// 0b上下左右;
    private String btnSendData = "";

    // Note: choose button or sensor
    private boolean isButtonControl = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Note: Bluetooth init
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "錯誤!! 這個裝置沒有藍芽", Toast.LENGTH_LONG).show();
            System.exit(0);
        }
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
            isBluetoothOpenByMe = true;
        }

        bluetoothThreadHandler = new Handler() {     //Bluetooth thread event
            TextView textView = (TextView) findViewById(R.id.bluetoothStatus);
            TextView buttonView = (TextView) findViewById(R.id.bluetoothConnectBtn);

            @Override
            public void handleMessage(Message msg) {
                byte[] data;

                switch (msg.what) {
                    case BluetoothConnectThread.MessageDelivered:
                        data = (byte[]) msg.obj;
                        //??????
                        break;
                    case BluetoothConnectThread.CONNECTING:
                        textView.setText("連線中....");
                        break;
                    case BluetoothConnectThread.CONNECTED:
                        textView.setText("已連線到" + bluetoothConnectThread.getDeviceAddress());
                        buttonView.setText("已經連線到" + bluetoothConnectThread.getDeviceName());
                        break;
                    case BluetoothConnectThread.DISCONNECT:
                        textView.setText("斷線");
                        buttonView.setText("藍芽連接");
                        break;
                    case BluetoothConnectThread.DISCONNECT_ON_ERROR:
                        Toast.makeText(MainActivity.this, ((Exception) msg.obj).getMessage(), Toast.LENGTH_SHORT).show();
                        textView.setText("錯誤斷線\n" + ((Exception) msg.obj).getMessage());
                        buttonView.setText("藍芽連接");
                        break;
                }
            }
        };

        // NOTE: Button event handler
        ImageButton upBtn = (ImageButton) findViewById(R.id.upBtn);
        upBtn.setOnTouchListener(new View.OnTouchListener() {
            boolean isDown = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isDown == false) {
                            btnStatus |= 0b1000;
                            isDown = true;
                            updateSendingData();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isDown == true) {
                            btnStatus &= ~(0b1000);
                            isDown = false;
                            updateSendingData();
                        }
                        break;
                }
                return false;
            }
        });

        ImageButton downBtn = (ImageButton) findViewById(R.id.downBtn);
        downBtn.setOnTouchListener(new View.OnTouchListener() {
            boolean isDown = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isDown == false) {
                            btnStatus |= 0b0100;
                            isDown = true;
                            updateSendingData();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isDown == true) {
                            btnStatus &= ~(0b0100);
                            isDown = false;
                            updateSendingData();
                        }
                        break;
                }
                return false;
            }
        });

        ImageButton leftBtn = (ImageButton) findViewById(R.id.leftBtn);
        leftBtn.setOnTouchListener(new View.OnTouchListener() {
            boolean isDown = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isDown == false) {
                            btnStatus |= 0b0010;
                            isDown = true;
                            updateSendingData();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isDown == true) {
                            btnStatus &= ~(0b0010);
                            isDown = false;
                            updateSendingData();
                        }
                        break;
                }
                return false;
            }
        });

        ImageButton rightBtn = (ImageButton) findViewById(R.id.rightBtn);
        rightBtn.setOnTouchListener(new View.OnTouchListener() {
            boolean isDown = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (isDown == false) {
                            btnStatus |= 0b0001;
                            isDown = true;
                            updateSendingData();
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        if (isDown == true) {
                            btnStatus &= ~(0b0001);
                            isDown = false;
                            updateSendingData();
                        }
                        break;
                }
                return false;
            }
        });

        // Note: 選擇 sensor 或是 button
        RadioGroup radioGroup = ((RadioGroup) findViewById(R.id.controlOptRadioGroup));
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                switch (checkedId) {
                    case R.id.btnControlOpt:
                        isButtonControl = true;
                        setSensorEnable(false);
                        break;
                    case R.id.sensorControlOpt:
                        isButtonControl = false;
                        setSensorEnable(true);
                        break;
                }
            }
        });
        radioGroup.check(R.id.btnControlOpt);

        /*
        //Note: 校正桿handler
        SeekBar seekBar = (SeekBar)findViewById(R.id.seekBar);
        seekBar.
        */
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_DEVICE_LIST:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra("deviceAddress");
                    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
                    Toast.makeText(this, "Connect to " + address, Toast.LENGTH_LONG).show();
                    bluetoothConnectThread = new BluetoothConnectThread(bluetoothDevice, bluetoothThreadHandler, 100){
                        @Override
                        public String onRequestSendingData() {
                            return null;
                        }
                    };
                    bluetoothConnectThread.start();
                } else {
                    Toast.makeText(this, "Connect Cancelled", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public void bluetoothConnectBtn(View view) {
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
        if (bluetoothConnectThread != null && bluetoothConnectThread.isAlive()) {
            Dialog dialog = new AlertDialog.Builder(this)
                    .setTitle("藍芽已經連線了")
                    .setMessage("確定要結束連線嗎????")
                    .setPositiveButton("結束連線", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            final Dialog dialog1 = ProgressDialog.show(MainActivity.this, "請稍後...", "斷線中...", true, false);
                            new Thread() {
                                @Override
                                public void run() {
                                    bluetoothConnectThread.setNeedStop(true);
                                    try {
                                        bluetoothConnectThread.join();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    dialog1.dismiss();
                                    bluetoothConnectThread = null;
                                }
                            }.start();
                        }
                    })
                    .setNegativeButton("我還要玩不要斷線", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Note: 取消不用做任何事
                        }
                    }).setCancelable(true).create();
            dialog.show();
        } else {
            Intent intent = new Intent(this, FindDevice.class);
            startActivityForResult(intent, REQUEST_DEVICE_LIST);
        }
    }

    private void updateSendingData() {
        switch (this.btnStatus) {
            case 0b0000:
            case 0b1100:
            case 0b0011:
            case 0b1111:
                btnSendData = "W255255";//不動
                break;
            case 0b1000:
            case 0b1011:
                btnSendData = "W510510";
                break;
            case 0b0100:
            case 0b0111:
                btnSendData = "W000000";
                break;
            case 0b0010:
            case 0b1110:
                btnSendData = "W000510";
                break;
            case 0b0001:
            case 0b1101:
                btnSendData = "W510000";
                break;
            case 0b1010:
                btnSendData = "W255510";
                break;
            case 0b1001:
                btnSendData = "W510255";
                break;
            case 0b0110:
                btnSendData = "W??????";
                break;
            case 0b0101:
                btnSendData = "W??????";
                break;
        }
        TextView text = (TextView) findViewById(R.id.btnStatusView);
        text.setText(btnSendData);
    }


    public void autoModeBtnOnClick(View view) {
        btnSendData = "W510000";
        TextView text = (TextView) findViewById(R.id.btnStatusView);
        text.setText(btnSendData);
    }

    private void setSensorEnable(boolean isUseSensor) {
        if (isUseSensor) {
            if (sensorManager == null) {
                // Note: Sensor enable
                System.out.println("啟動Sensor");
                sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                sensorEventListener = new SensorEventListener() {
                    private TextView textView = (TextView) findViewById(R.id.sensorStatus);
                    private float[] accelData = null;
                    private float[] magFieldData = null;
                    private float[] orientData = null;
                    private float[] matR = new float[9];

                    @Override
                    public void onSensorChanged(SensorEvent event) {

                        switch (event.sensor.getType()) {
                            case Sensor.TYPE_ACCELEROMETER:
                                accelData = event.values;
                                break;
                            case Sensor.TYPE_MAGNETIC_FIELD:
                                magFieldData = event.values;
                                break;
                            case Sensor.TYPE_ORIENTATION:
                                orientData = event.values;
                                break;
                        }
                        if (accelData != null && magFieldData != null) {
                            boolean isSensorOk = SensorManager.getRotationMatrix(matR, null, accelData, magFieldData);
                            if (isSensorOk) {
                                textView.setText("");
                                float[] orientation = SensorManager.getOrientation(matR, new float[3]);
                                for (float tmp : orientation) {
                                    textView.append(String.format("%-7.1f", tmp / Math.PI * 180));
                                }
                            }
                            accelData = null;
                            magFieldData = null;
                        }
                        if (orientData != null) {
                            textView.setText("");
                            for (float tmp : orientData) {
                                textView.append(String.format("%-7.1f", tmp));
                            }
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    }

                };
                try {
                    sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);
                    sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
                } catch (Exception ex) {
                    System.out.println(ex);
                    System.out.println("取得Sensor失敗 退回過時方法");
                    sensorManager.registerListener(sensorEventListener, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_GAME);
                }
            } else {
                //Note: Sensor 已經開過了?
            }
        } else {
            if (sensorManager != null) {
                System.out.println("取消註冊Sensor");
                sensorManager.unregisterListener(sensorEventListener);
                sensorManager = null;
                sensorEventListener = null;
            } else {
                //Note: 已經關過了?
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothConnectThread != null && bluetoothConnectThread.isAlive()) {
            System.out.println("發現藍芽還連線著 把它斷線...");
            bluetoothConnectThread.setNeedStop(true);
            try {
                bluetoothConnectThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            bluetoothConnectThread = null;
        }

        // Note: 如果現在是用sensor控制 那要記得把sensor關掉
        if (!isButtonControl) {
            isButtonControl = true;
            this.setSensorEnable(false);
        }

        if (isBluetoothOpenByMe) {
            System.out.println("藍芽是我開的 把他關起來...");
            bluetoothAdapter.disable();
        }
    }

}
