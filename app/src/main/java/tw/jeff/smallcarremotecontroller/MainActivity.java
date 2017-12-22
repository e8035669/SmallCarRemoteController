package tw.jeff.smallcarremotecontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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

    // Note: 小車命令計算機
    private WheelCommand wheelCommand = new WheelCommand();

    //Note: Debug
    private boolean debug = false;
    private int debugCount = 0;


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
                        String address = "";
                        String name = "";
                        if(bluetoothConnectThread != null) {
                            address = bluetoothConnectThread.getDeviceAddress();
                            name = bluetoothConnectThread.getDeviceName();
                        }
                        textView.setText("已連線到" + address);
                        buttonView.setText("已經連線到" + name);
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


        //Note: 校正桿handler
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            Toast toast = Toast.makeText(MainActivity.this, "??", Toast.LENGTH_SHORT);

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                wheelCommand.setMaxSpeed(progress - 128);
                toast.setText(Integer.toString(progress - 128));
                if (fromUser) {
                    toast.show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        int value = preferences.getInt("seekbar", 128);
        seekBar.setProgress(value);

        // Note: Debug 模式
        debug = preferences.getBoolean("debug", false);
        LinearLayout debugView = (LinearLayout) findViewById(R.id.debugView);
        Button autoModeBtn = (Button) findViewById(R.id.autoModeBtn);
        if (debug) {
            debugView.setVisibility(View.VISIBLE);
            autoModeBtn.setVisibility(View.VISIBLE);
        } else {
            debugView.setVisibility(View.GONE);
            autoModeBtn.setVisibility(View.GONE);
        }
        Button button = (Button) findViewById(R.id.bluetoothConnectBtn);
        button.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                debugCount++;
                if (debugCount == 10) {
                    debugCount = 0;
                    debug = !debug;
                    LinearLayout debugView = (LinearLayout) findViewById(R.id.debugView);
                    Button autoModeBtn = (Button) findViewById(R.id.autoModeBtn);
                    if (debug) {
                        debugView.setVisibility(View.VISIBLE);
                        autoModeBtn.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "恭喜你 現在可以debug了!", Toast.LENGTH_SHORT).show();
                    } else {
                        debugView.setVisibility(View.GONE);
                        autoModeBtn.setVisibility(View.GONE);
                    }
                }
                return false;
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_DEVICE_LIST:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getStringExtra("deviceAddress");
                    final BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
                    //Toast.makeText(MainActivity.this, "Connect to " + address, Toast.LENGTH_LONG).show();
                    final Dialog dialog1 = ProgressDialog.show(MainActivity.this, "請稍後...", "正在連線到" + bluetoothDevice.getName(), true, false);
                    new Thread() {
                        @Override
                        public void run() {
                            bluetoothConnectThread = new BluetoothConnectThread(bluetoothDevice, bluetoothThreadHandler, 100) {
                                @Override
                                public String onRequestSendingData() {
                                    if (isButtonControl) {
                                        return btnSendData;
                                    } else {
                                        return sensorSendData;
                                    }
                                }
                            };
                            bluetoothConnectThread.start();
                            dialog1.dismiss();
                        }
                    }.start();

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
        double left = 0;
        double right = 0;
        switch (this.btnStatus) {
            case 0b0000:
            case 0b1100:
            case 0b0011:
            case 0b1111:
                left = 0;//不動
                right = 0;
                break;
            case 0b1000:
            case 0b1011:
                left = 1;
                right = 1;
                break;
            case 0b0100:
            case 0b0111:
                left = -1;
                right = -1;
                break;
            case 0b0010:
            case 0b1110:
                left = -1;
                right = 1;
                break;
            case 0b0001:
            case 0b1101:
                left = 1;
                right = -1;
                break;
            case 0b1010:
                left = 0;
                right = 1;
                break;
            case 0b1001:
                left = 1;
                right = 0;
                break;
            case 0b0110:
                left = 0;
                right = -1;
                break;
            case 0b0101:
                left = -1;
                right = 0;
                break;
        }
        btnSendData = wheelCommand.getCommand(left, right);
        TextView text = (TextView) findViewById(R.id.btnStatusView);
        text.setText(btnSendData);
    }


    public void autoModeBtnOnClick(View view) {
        btnSendData = wheelCommand.getCommand(1, -1);
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

                        if (orientData != null) {
                            textView.setText("");
                            //for (float tmp : orientData) {
                            //    textView.append(String.format("%-7.1f", tmp));
                            //}
                            double left = ((orientData[1] + 30) / 45) - (orientData[2] / 45);
                            double right = ((orientData[1] + 30) / 45) + (orientData[2] / 45);
                            sensorSendData = wheelCommand.getCommand(left, right);
                            textView.append(sensorSendData);
                            orientData = null;
                        } else if (accelData != null && magFieldData != null) {
                            boolean isSensorOk = SensorManager.getRotationMatrix(matR, null, accelData, magFieldData);
                            if (isSensorOk) {
                                textView.setText("");
                                float[] orientation = SensorManager.getOrientation(matR, new float[3]);
                                //for (float tmp : orientation) {
                                //    textView.append(String.format("%-7.1f", tmp / Math.PI * 180));
                                //}
                                double left = ((orientation[1] + Math.PI / 6) / Math.PI * 4) + (orientation[2] / Math.PI * 4);
                                double right = ((orientation[1] + Math.PI / 6) / Math.PI * 4) - (orientation[2] / Math.PI * 4);
                                sensorSendData = wheelCommand.getCommand(left, right);
                                textView.append(sensorSendData);
                            }
                            accelData = null;
                            magFieldData = null;
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

        SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekBar);
        int data = seekBar.getProgress();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("seekbar", data);
        editor.putBoolean("debug", debug);
        editor.commit();
    }


}
