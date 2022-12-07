package com.example.networkconnectionapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.*;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;


public class MainActivity extends AppCompatActivity {
//    Provides access to information about the telephony services on the device.
//    Applications can use the methods in this class to determine telephony services and
//    states, as well as to access some types of subscriber information.
    private TelephonyManager telephonyManager;
    //used for call backs
    private PhoneCustomStateListener psListener;
    private TextView textViewNetworkType;
    private TextView textViewNetworkStrength;
    private TextView textviewWifi;
    private TextView timeTaken;
    private ProgressDialog progressDialog;
//    bitmap is used to store images
    private Bitmap bitmap = null;
    private long start;
    private long end;
    Button b1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //turns off the dark mode
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);
        b1 = (Button) findViewById(R.id.button);
        textViewNetworkType = (TextView) findViewById(R.id.tv_network_type);
        textViewNetworkStrength = (TextView) findViewById(R.id.tv_network_strength);
        textviewWifi = findViewById(R.id.tv_wifi);
        timeTaken=findViewById(R.id.time_taken);

        psListener = new PhoneCustomStateListener();
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //will keep on listening when ever the system changes.
        telephonyManager.listen(psListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeTaken.setText(String.valueOf(0));
                start=0;
                //gets a boolean that whether the network is connected or not
                boolean status=NetworkManagerUtils.checkInternetConnection(MainActivity.this);
                //simply downloads an image using a new thread
                if(status==false) return;
//                start=System.currentTimeMillis();
                downloadImage("https://wallpaperaccess.com/full/300068.jpg");

                // just gets the type of network.
                String networkType = NetworkManagerUtils.getNetworkClass(MainActivity.this);

                textViewNetworkType.setText(networkType);
                //this will happen if the device is connected to WIFI.
                if(NetworkManagerUtils.isConnectedWifi(MainActivity.this)){
                    // simply makes a object of wifiManager
                    //get system service : Interface to global information about an application environment. Provided by the android itself.
                    WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    int numberOfLevels = 5;

                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    //RSSI : Received signal strength indicator.
                    int level = WifiManager.calculateSignalLevel(wifiInfo.getRssi(), numberOfLevels);
                    if (level == 4) {
                        textviewWifi.setText("Wifi Signal : Great");
                    } else if (level == 3) {
                        textviewWifi.setText("Wifi Signal : Good");
                    } else if (level == 2) {
                        textviewWifi.setText("Wifi Signal : Moderate");
                    } else if (level == 1) {
                        textviewWifi.setText("Wifi Signal : Poor");
                    }else{
                        textviewWifi.setText("Wifi Signal : Very weak");
                    }
                }else{
                    textviewWifi.setText("No Wifi Connected");
                }
            }
        });
    }

    private void downloadImage(String urlStr) {
        progressDialog = ProgressDialog.show(this, "", "Downloading Image from " + urlStr);
        final String url = urlStr;

        new Thread() {
            public void run() {
                InputStream in = null;
                // just to handle message transfer
                Message msg = Message.obtain();
                msg.what = 1;
//IO exception -
                try {

                    // properties of http connection
                    start=System.currentTimeMillis();
                    in = openHttpConnection(url);
                    bitmap = BitmapFactory.decodeStream(in);
                    end=System.currentTimeMillis();
                    timeTaken.setText(String.valueOf(end-start));
                    // to transfer data from on
                    Bundle b = new Bundle();
                    // extends
                    b.putParcelable("bitmap", bitmap);
                    msg.setData(b);
                    if (in != null) {
//                        end=.currentTimeMillis()
                        in.close();
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                messageHandler.sendMessage(msg);
            }
        }.start();
    }

    private InputStream openHttpConnection(String urlStr) {
        InputStream in = null;
        int resCode = -1;

        try {
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }

            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            // interaction is not allowed
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            resCode = httpConn.getResponseCode();

            if (resCode == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    private Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ImageView img = (ImageView) findViewById(R.id.imageView);
            img.setImageBitmap((Bitmap) (msg.getData().getParcelable("bitmap")));
            progressDialog.dismiss();
        }
    };

    public class PhoneCustomStateListener extends PhoneStateListener {

        public int signalSupport = 0;

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);

            int signalLevel = signalStrength.getLevel();
//            Log.d(getClass().getCanonicalName(), "------ gsm signal --> " + signalSupport);

            if (signalLevel == 4) {
                textViewNetworkStrength.setText("Mobile Signal : Great");


            } else if (signalLevel == 3) {
                textViewNetworkStrength.setText("Mobile Signal : Good");


            } else if (signalLevel == 2) {
                textViewNetworkStrength.setText("Mobile Signal : Moderate");


            } else if (signalLevel == 1) {
                textViewNetworkStrength.setText("Mobile Signal : Poor");

            }else{
                textViewNetworkStrength.setText("Mobile Signal : Very weak");
            }
        }
    }
}