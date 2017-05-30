package tw.jeff.smallcarremotecontroller;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * Created by Jeff on 2017/5/23.
 */

public abstract class BluetoothConnectThread extends Thread {
    public static final int NONE = 0;
    public static final int CONNECTED = 1;
    public static final int CONNECTING = 2;
    public static final int DISCONNECT = 3;
    public static final int DISCONNECT_ON_ERROR = 4;
    public static final int MessageDelivered = 10;
    private final Object statusMutex = new Object();
    private final UUID SSPUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothDevice device;
    private BluetoothSocket socket = null;
    private int status = BluetoothConnectThread.NONE;
    private int delayTime = 0;
    private InputStream inputStream = null;
    private PrintWriter outputWriter = null;
    private long timeStamp = 0;
    private Handler messageHandler;
    private boolean isNeedStop = false;

    public BluetoothConnectThread(BluetoothDevice device, Handler messageHandler, int delayTime) {
        this.device = device;
        this.delayTime = delayTime;
        this.messageHandler = messageHandler;
        setStatus(BluetoothConnectThread.CONNECTING);
        try {
            //Method method = device.getClass().getMethod("createRfcommSocket", int.class);
            //socket = (BluetoothSocket) method.invoke(device, Integer.valueOf(1));
            socket = device.createRfcommSocketToServiceRecord(SSPUUID);
            socket.connect();
            inputStream = socket.getInputStream();
            outputWriter = new PrintWriter(socket.getOutputStream());
            setStatus(BluetoothConnectThread.CONNECTED);
        } catch (IOException e) {
            setStatusErr(DISCONNECT_ON_ERROR, e);
        } catch (Exception ex) {
            setStatusErr(DISCONNECT_ON_ERROR, ex);
        }
        timeStamp = System.currentTimeMillis();
    }


    @Override
    public void run() {
        byte[] buffer = new byte[1024];
        Exception ex = null;
        while (getStatus() == CONNECTED && !isNeedStop) {
            try {
                if (inputStream.available() > 0) {
                    int length = inputStream.read(buffer);
                    messageHandler.obtainMessage(MessageDelivered, length, -1, buffer).sendToTarget();
                }
            } catch (IOException e) {
                ex = e;
            }

            if (System.currentTimeMillis() > (timeStamp + delayTime)) {
                timeStamp = System.currentTimeMillis();
                try {
                    socket.getOutputStream().write(onRequestSendingData().getBytes());
                } catch (IOException e) {
                    ex = e;
                }
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (ex == null) {
            setStatus(BluetoothConnectThread.DISCONNECT);
        } else {
            setStatusErr(BluetoothConnectThread.DISCONNECT_ON_ERROR, ex);
        }
    }

    public int getStatus() {
        int tmp = 0;
        synchronized (this.statusMutex) {
            tmp = status;
        }
        return tmp;
    }

    private void setStatus(int status) {
        synchronized (this.statusMutex) {
            this.status = status;
        }
        messageHandler.obtainMessage(this.status).sendToTarget();
    }

    private void setStatusErr(int status, Exception ex) {
        synchronized (this.statusMutex) {
            this.status = status;
        }
        messageHandler.obtainMessage(this.status, ex).sendToTarget();
    }

    public void setNeedStop(boolean needStop) {
        isNeedStop = needStop;
    }

    public String getDeviceName() {
        return device.getName();
    }

    public String getDeviceAddress() {
        return device.getAddress();
    }

    public abstract String onRequestSendingData();
}
