package com.example.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private TextView mStatus;

    private BluetoothAdapter mBluetoothAdapter;
    private ArrayAdapter<String> mDevicesItemArrayAdapter;
    private String mName;
    private String mAddress;

    // bluetooth background worker thread to send and receive data
    private ConnectedThread mConnectedThread;
    private ConnectThread mConnectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStatus = findViewById(R.id.Status);
        Button go = findViewById(R.id.go);
        Button right = findViewById(R.id.right);
        Button left = findViewById(R.id.left);
        Button back = findViewById(R.id.back);
        Button stop = findViewById(R.id.stop);
        go.setOnClickListener(this);
        right.setOnClickListener(this);
        left.setOnClickListener(this);
        back.setOnClickListener(this);
        stop.setOnClickListener(this);

        checkOn();
        askConnect();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.go:
                mConnectedThread.write("A");
                break;
            case R.id.right:
                mConnectedThread.write("D");
                break;
            case R.id.left:
                mConnectedThread.write("C");
                break;
            case R.id.back:
                mConnectedThread.write("B");
                break;
            case R.id.stop:
                mConnectedThread.write("I");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnectThread.cancel();
        mConnectedThread.cancel();
    }

    private void checkOn() {
        //判断蓝牙是否打开
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            while (true) {
                AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
                mDialog.setTitle("Can't open Program.")
                        .setMessage("Bluetooth isn't on.\nTry again later.")
                        .setPositiveButton("Fire", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setCancelable(false)
                        .show();
            }
        }

    }

    private void askConnect() {
        //询问是否已连接设备
        AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
        mDialog.setTitle("Have you connected the device?")
                .setMessage("\"Yes\" to go on.\n\"No\" to close and try again later.")
                .setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selectDevice();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void selectDevice() {
        //已在系统设置里连接设备，因此只需要找到连接的设备。但是无法查询已连接设备列表，所以在此手动在已配对设备中选择
        //把配对的设备导到数组中
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() < 1) {
            AlertDialog.Builder mDialog = new AlertDialog.Builder(this);
            mDialog.setTitle("Can't open Program.")
                    .setMessage("you connect no device.\nTry again later.")
                    .setPositiveButton("Fire", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
        mDevicesItemArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        for (BluetoothDevice device : pairedDevices)
            mDevicesItemArrayAdapter.add(device.getName() + "\n\t" + device.getAddress());
        //在对话框中显示与选择后再显示出来
        AlertDialog.Builder mDialog2 = new AlertDialog.Builder(this);
        mDialog2.setTitle("Select the device.")
                .setAdapter(mDevicesItemArrayAdapter, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String mDeviceString = mDevicesItemArrayAdapter.getItem(which);
                        mName = mDeviceString.substring(0, mDeviceString.indexOf("\n"));
                        mAddress = mDeviceString.substring(mDeviceString.indexOf("\t") + 1);
                        mStatus.setText(mName + "\n\t" + mAddress);
                        //建立传输信息通道
                        mConnectThread = new ConnectThread(mBluetoothAdapter.getRemoteDevice(mAddress));
                        mConnectThread.run();
                    }
                })
                .setNegativeButton("None", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }


    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        private BluetoothDevice mmDevice;

        ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(BT_UUID);
            } catch (IOException ignored) { }
            mmSocket = tmp;
        }

        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                //下面处理是在 StackOverflow 找的，貌似没用上
                Log.e("Socket connect:",connectException.getMessage());
                try {
                    Log.e("","trying fallback...");
                    mmSocket =(BluetoothSocket) mmDevice.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(mmDevice,1);
                    mmSocket.connect();
                    Log.e("","Connected");
                } catch (Exception e2) {
                    Log.e("", "Couldn't establish Bluetooth connection!\n" + e2.getMessage());
                    try {
                        mmSocket.close();
                    } catch (IOException ignored) { }
                    return;
                }
            }
            Toast.makeText(getBaseContext(), "Connected",
                    Toast.LENGTH_SHORT).show();
            // Do work to manage the connection (in a separate thread)
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        /** Will cancel an in-progress connection, and close the socket */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            OutputStream tmpOut = null;

            // Get the output streams, using temp objects because
            // member streams are final
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) { }

            mmOutStream = tmpOut;
        }

        /* Call this from the main activity to send data to the remote device */
        void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                    mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("OutStream write:",e.getMessage());
            }
        }

        /* Call this from the main activity to shutdown the connection */
        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException ignored) { }
        }
    }
}