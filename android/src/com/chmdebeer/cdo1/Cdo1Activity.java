package com.chmdebeer.cdo1;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.github.nkzawa.socketio.client.*; // java socket io client
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.emitter.Emitter;
import org.json.JSONException;
import org.json.JSONObject;


public class Cdo1Activity extends Activity implements OnSeekBarChangeListener, Runnable {

    private TextView textViewColor;

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

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(accessory != null){
                            //call method to set up accessory communication
                            openAccessory();
                        }
                    }
                    else {
                        //Log.d(TAG, "permission denied for accessory " + accessory);
                        Toast.makeText(context, "permission denied for accessory "+accessory, Toast.LENGTH_SHORT).show();
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

    private static final int SERVERPORT = 80;
    private static final String SERVER_IP = "cdo1.chmdebeer.com";
    private Socket socket; // socket object

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        linearLayout = (LinearLayout) findViewById(R.id.linearLayout);

        textViewColor = (TextView) findViewById(R.id.textViewColor);

        seekBarRed = (SeekBar) findViewById(R.id.seekBarRed);
        seekBarGreen = (SeekBar) findViewById(R.id.seekBarGreen);
        seekBarBlue = (SeekBar) findViewById(R.id.seekBarBlue);

        seekBarBlue.setOnSeekBarChangeListener(this);
        seekBarGreen.setOnSeekBarChangeListener(this);
        seekBarRed.setOnSeekBarChangeListener(this);

        textViewColor.setText(String.format("(%d, %d, %d)", red, green, blue));
        linearLayout.setBackgroundColor(Color.rgb(red, green, blue));

        try
        {
            socket = IO.socket("http://cdo1.chmdebeer.com:80");
            socket.connect();  // initiate connection to socket server
            socket.emit("chat message",  "From Android to server: 1st outgoing message");
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
                    Double d = obj.getDouble("speed") * 100.0;
                    seekBarRed.setProgress(d.intValue());
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

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener()
        {
            @Override
            public void call(Object... arg0) {
                // TODO Auto-generated method stub
                Log.d("socketio", "socket event connect error");
                socket.emit("chat message",  "android to server: socket event connect error");
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

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Toast.makeText(this, "USB Manager = "+mUsbManager, Toast.LENGTH_SHORT).show();

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);
        Toast.makeText(this, "Intent Registered", Toast.LENGTH_SHORT).show();

        if(mUsbManager.hasPermission(mAccessory)) {
            Toast.makeText(this, "Accessory has permission", Toast.LENGTH_SHORT).show();
            unregisterReceiver(mUsbReceiver);
            openAccessory();
        } else {
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

    @SuppressLint("NewApi")
    private void openAccessory() {
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int value, boolean flag) {
        if(seekBar == seekBarRed) {
            red = (byte) value;
        } else if(seekBar == seekBarGreen) {
            green = (byte) value;
        } else if(seekBar == seekBarBlue) {
            blue = (byte) value;
        }
        linearLayout.setBackgroundColor(Color.rgb(red & 0xFF, green & 0xFF, blue & 0xFF));
        textViewColor.setText(String.format("(%d, %d, %d)", red & 0xFF, green & 0xFF, blue & 0xFF));

        if(mOutputStream != null) {
            try {
                byte[] data = new byte[4];
                data[0] = 0x2;
                data[1] = red;
                data[2] = green;
                data[3] = blue;
                mOutputStream.write(data);
                mOutputStream.flush();
            } catch (IOException e) {
                Toast.makeText(this, "Error "+e, Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onStartTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onStopTrackingTouch(SeekBar arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void run() {

    }

}
