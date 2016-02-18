package com.chmdebeer.cdo1;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

//import org.json.JSONException;
//import org.json.JSONObject;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;

import org.json.JSONException;
import org.json.JSONObject;

/*
When debugging applications that use USB accessory or host features, you most likely will have USB hardware connected to your Android-powered device. This will prevent you from having an adb connection to the Android-powered device via USB. You can still access adb over a network connection. To enable adb over a network connection:

    Connect the Android-powered device via USB to your computer.
    From your SDK platform-tools/ directory, enter adb tcpip 5555 at the command prompt.
    Enter adb connect <device-ip-address>:5555 You should now be connected to the Android-powered device and can issue the usual adb commands like adb logcat.
    adb connect 192.168.1.67:5555
    To set your device to listen on USB, enter adb usb.

    adb logcat -s usb

*/

public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private TextView textViewColor;
    private TextView logBox;

    private SeekBar seekBarRed;

    private SeekBar seekBarGreen;

    private SeekBar seekBarBlue;

    private byte red, green, blue;

    private LinearLayout linearLayout;

    private UsbAccessory mAccessory;

    private ParcelFileDescriptor mFileDescriptor;

    private FileInputStream mInputStream;

    private FileOutputStream mOutputStream;

    private UsbManager mUsbManager;

    private Socket socket; // socket object

    private static final String ACTION_USB_PERMISSION = "com.chmdebeer.cdo1.USB_PERMISSION";

    public static void LogToView(TextView logTextView, String title, String message) {
        Log.d(title, message);
        logTextView.setText(logTextView.getText() + title + ": " + message + "\n");
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogToView(logBox, "usb", "action: " + action);
            Toast.makeText(context, "action: " + action, Toast.LENGTH_SHORT).show();
            if (ACTION_USB_PERMISSION.equals(action)) {
                LogToView(logBox, "usb", "action matched");
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(accessory != null){
                            LogToView(logBox, "usb", "openAccessory()");
                            //call method to set up accessory communication
                            openAccessory();
                        }
                    }
                    else {
                        LogToView(logBox, "usb", "permission denied for accessory " + accessory);
                        Toast.makeText(context, "permission denied for accessory " + accessory, Toast.LENGTH_SHORT).show();
                    }
                }
                unregisterReceiver(mUsbReceiver);
            }

        }
    };

    public final BroadcastReceiver mUsbDetachReciever = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogToView(logBox, "usb", "onReceive " + action);
            if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                UsbAccessory accessory = (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null) {
                    cleanUp();
                }
                unregisterReceiver(mUsbDetachReciever);
            }

        }

    };



    private boolean mPermissionRequestPending;

    private void cleanUp() {
        try {
            if(mFileDescriptor != null)
                mFileDescriptor.close();

            if(mInputStream != null)
                mInputStream.close();

            if(mOutputStream != null)
                mOutputStream.close();
            mAccessory = null;
            mUsbManager = null;
            Toast.makeText(this, "Closed all resources", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error occured "+e, Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);

        textViewColor = (TextView) findViewById(R.id.textViewColor);
        logBox = (TextView) findViewById(R.id.textView2);
        logBox.setMovementMethod(new ScrollingMovementMethod());

        seekBarRed = (SeekBar) findViewById(R.id.seekBarRed);
        seekBarGreen = (SeekBar) findViewById(R.id.seekBarGreen);
        seekBarBlue = (SeekBar) findViewById(R.id.seekBarBlue);

        seekBarBlue.setOnSeekBarChangeListener(this);
        seekBarGreen.setOnSeekBarChangeListener(this);
        seekBarRed.setOnSeekBarChangeListener(this);

        textViewColor.setText(String.format("(%d, %d, %d)", red, green, blue));
//        linearLayout.setBackgroundColor(Color.rgb(red, green, blue));

        try
        {
            socket = IO.socket("https://www.chmdebeer.ca");
            socket.connect();  // initiate connection to socket server
            socket.emit("echo",  "From Android to server: 1st outgoing message");
        }
        catch (URISyntaxException e)
        {
            e.printStackTrace();
        }

        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener()
        {

            @Override
            public void call(Object... args) {
                Log.d("socketio", "socket connected");
                socket.emit("echo", "even connect: message sent from android to socketio server");
                //socket.disconnect(); // why is there a disconnect here?
            }
        }).on("log-message", new Emitter.Listener() {

            @Override
            public void call(Object... arg0) {
                // TODO Auto-generated method stub
                JSONObject obj = (JSONObject)arg0[0];
                Log.d("socketio", "Log Message: "+obj.toString());
            }
        }).on("heartbeat", new Emitter.Listener() {

            @Override
            public void call(Object... arg0) {
                // TODO Auto-generated method stub
                JSONObject obj = (JSONObject)arg0[0];
                try {
                    int p = obj.getInt("power");
                    int d = obj.getInt("direction");

                    if(mOutputStream != null) {
                        try {
                            byte[] data = new byte[4];
                            data[0] = 0x02;
                            data[1] = (byte)(p & 0xFF);
                            data[2] = (byte)(d & 0xFF);
                            data[3] = (byte)((d>>8) & 0xFF);
                            Log.d("usb", String.format("power %d, direction %d (%d, %d, %d)", p, d, data[1], data[2], data[3]));
                            mOutputStream.write(data);
                            mOutputStream.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    seekBarRed.setProgress((p*100)/255);
                    seekBarGreen.setProgress((d*100)/360);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("socketio", "Heartbeat: "+obj.toString());
            }
        }).on(Socket.EVENT_MESSAGE, new Emitter.Listener() {

            @Override
            public void call(Object... arg0) {
                // TODO Auto-generated method stub
                Log.d("socketio", "socket event message" + arg0);
                //socket.emit("chat message", "android to server from event message");
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... arg0) {
                // TODO Auto-generated method stub
                Log.d("socketio", "socket event connect error");
                socket.emit("chat message", "android to server: socket event connect error");
            }
        });

        socket.on(Socket.EVENT_MESSAGE, new Emitter.Listener() {

            @Override
            public void call(Object... arg0) {
                // TODO Auto-generated method stub
                Log.d("socketio", "socket event message" + arg0);
                socket.emit("chat message", "android to server from event message");
            }
        });

        mAccessory = (UsbAccessory) getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
        Toast.makeText(this, "USB Accessory = " + mAccessory, Toast.LENGTH_SHORT).show();
        Log.d("usb", "USB Accessory = " + mAccessory);

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Toast.makeText(this, "USB Manager = "+mUsbManager, Toast.LENGTH_SHORT).show();
        Log.d("usb", "USB Manager = "+mUsbManager);

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        Toast.makeText(this, "Intent Registered", Toast.LENGTH_SHORT).show();
        Log.d("usb", "Intent Registered");

        UsbAccessory[] accessoryList = mUsbManager.getAccessoryList();
        if (accessoryList == null) {
            Toast.makeText(this, "No accessories detected!", Toast.LENGTH_SHORT).show();
            Log.d("usb", "No list of accessories!");
        } else {
            Log.d("usb", "found " + accessoryList[0].toString());
        }

        if(mUsbManager.hasPermission(mAccessory)) {
            Toast.makeText(this, "Accessory has permission", Toast.LENGTH_SHORT).show();
            unregisterReceiver(mUsbReceiver);
            openAccessory();
        } else {
            LogToView(logBox, "usb", "Accessory has no permission");
            Toast.makeText(this, "Accessory has no permission", Toast.LENGTH_SHORT).show();
            synchronized (mUsbReceiver) {
                if (!mPermissionRequestPending) {
                    mUsbManager.requestPermission(mAccessory, mPermissionIntent);
                    Toast.makeText(this, "Permission Request Sent", Toast.LENGTH_SHORT).show();
                    mPermissionRequestPending = true;
                }
            }
        }

        IntentFilter filter2 = new IntentFilter(ACTION_USB_PERMISSION);
        filter2.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        registerReceiver(mUsbDetachReciever, filter2);


    }

    public void onDestroy() {
        super.onDestroy();
        cleanUp();

    }

    private void openAccessory() {
        LogToView(logBox, "usb", "called openAccessory()");
        //Log.d(TAG, "openAccessory: " + accessory);

        mFileDescriptor = mUsbManager.openAccessory(mAccessory);
        Toast.makeText(this, "Accessory Opened", Toast.LENGTH_SHORT).show();
        if (mFileDescriptor != null) {
            FileDescriptor fd = mFileDescriptor.getFileDescriptor();
            Toast.makeText(this, "File Desc "+fd, Toast.LENGTH_SHORT).show();
            mInputStream = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

        }

    }

    public void onProgressChanged(SeekBar seekBar, int value, boolean flag) {
        if(seekBar == seekBarRed) {
            red = (byte) value;
        } else if(seekBar == seekBarGreen) {
            green = (byte) value;
        } else if(seekBar == seekBarBlue) {
            blue = (byte) value;
        }
        //linearLayout.setBackgroundColor(Color.rgb(red & 0xFF, green & 0xFF, blue & 0xFF));

    }

    public void onStartTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub

    }

    public void onStopTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub

    }

}
