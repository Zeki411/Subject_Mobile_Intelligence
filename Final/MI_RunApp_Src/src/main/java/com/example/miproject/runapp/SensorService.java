package com.example.miproject.runapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements SensorEventListener {

    public static final String BROADCAST_SENSOR_RESULT = "com.example.miproject.runapp.BROADCAST_SENSOR_RESULT";
    public static final String EXTRA_SENSOR_RESULT = "com.example.miproject.runapp.EXTRA_SENSOR_RESULT";

    private SensorManager mSensorManager;
    Sensor mAccLinear;
    Sensor mGravity;
    Sensor mGyro;
    Sensor mAcc;
    Sensor mMag;


    public static String TAG = "Activity SensorService";
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    public float[] accValue = new float[3];
    public float[] gravityValue = new float[3];
    public float[] magValue = new float[3];
    public float[] gyroValue = new float[3];
    public float[] accLinValue = new float[3];
    public float[] rotationMatrix = new float[9];
    public float[] orientationAngles = new float[3];
    public float[] sendingArray = new float[12];          //Data frame
    public float[] dataToServer = new float[1536];


    public static ArrayList<float[]> sampledDataList = new ArrayList<float[]>();
//    public static ArrayList<float[]> dataToServer = new ArrayList<float[]>(128);


    private int[] dataPointCounter = new int[5];
    private int[] sensorComplete = new int[5];

    private long timestamp;
    private int firstTimeSample = 0;
    private int index, dataCount;

//    // Instantiate the RequestQueue.
//    RequestQueue queue = Volley.newRequestQueue(this);

//
//    // Request a string response from the provided URL.
//    StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
//            new Response.Listener<String>() {
//                @Override
//                public void onResponse(String response) {
//                    // Display the first 500 characters of the response string.
////                    textView.setText("Response is: "+ response.substring(0,500));
//
//                }
//            }, new Response.ErrorListener() {
//        @Override
//        public void onErrorResponse(VolleyError error) {
////            textView.setText("That didn't work!");
//        }
//    });

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void volleyPost(float[] data2Send) {
        String postUrl ="http://192.168.0.184:8080/results";
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        JSONObject postData = new JSONObject();
        try {
            postData.put("id", "0");
            postData.put("client_time", "0");
            JSONArray dataJson = new JSONArray(data2Send);
            postData.put("data", dataJson);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
//                System.out.println(response);
                try {
                    final Intent broadcast = new Intent(BROADCAST_SENSOR_RESULT);
                    broadcast.putExtra(EXTRA_SENSOR_RESULT, new String[] {response.getString("act"), response.getString("gender")});
                    LocalBroadcastManager.getInstance(SensorService.this).sendBroadcast(broadcast);

                } catch (JSONException e) {
                    e.printStackTrace();
                }



            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        requestQueue.add(jsonObjectRequest);
    }


    @Override
    public final void onCreate() {
        super.onCreate();

        startMeasurement();
        Log.d(TAG,"sensor service starting");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MapsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor Service")
                .setContentText(input)
                .setSmallIcon(R.mipmap.logo_round)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(100, notification);
        //do heavy work on a background thread
        //stopSelf();
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        stopMeasurement();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public final void onSensorChanged(SensorEvent sensorEvent) {
        long cycleTime = 0;

        if(firstTimeSample == 0)
        {
            timestamp = sensorEvent.timestamp;
            firstTimeSample = 1;
        }

        switch (sensorEvent.sensor.getType()) {

            case Sensor.TYPE_ACCELEROMETER:      //Only used for calculating rotation matrix
                System.arraycopy(sensorEvent.values, 0, accValue, 0, accValue.length);
                sensorComplete[0] = 1;
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:     //Only used for calculating rotation matrix
                System.arraycopy(sensorEvent.values, 0, magValue, 0, magValue.length);
                sensorComplete[1] = 1;
                break;

            case Sensor.TYPE_GYROSCOPE:          //Angular rate data [rad/s]
                System.arraycopy(sensorEvent.values, 0, gyroValue, 0, gyroValue.length);
                sendingArray[6] = gyroValue[0];  // X-axis
                sendingArray[7] = gyroValue[1];  // Y-axis
                sendingArray[8] = gyroValue[2];  // Z-axis
                sensorComplete[2] = 1;
                break;

            case Sensor.TYPE_GRAVITY:            //Gravity data [m/s^2]
                System.arraycopy(sensorEvent.values, 0, gravityValue, 0, gravityValue.length);
                sendingArray[3] = gravityValue[0];   // X-axis
                sendingArray[4] = gravityValue[1];   // Y-axis
                sendingArray[5] = gravityValue[2];   // Z-axis
                sensorComplete[3] = 1;
                break;

            case Sensor.TYPE_LINEAR_ACCELERATION:          // Acceleration data [m/s^2]
                System.arraycopy(sensorEvent.values, 0, accLinValue, 0, accLinValue.length);
                sendingArray[9]  = accLinValue[0];   // X-axis
                sendingArray[10] = accLinValue[1];   // Y-axis
                sendingArray[11] = accLinValue[2];   // Z-axis
                sensorComplete[4] = 1;
                break;
        }



        if(sensorComplete[0] == 1 && sensorComplete[1] == 1 &&
                sensorComplete[2] == 1 && sensorComplete[3] == 1 && sensorComplete[4] == 1)
        {
            sensorComplete[0] = 0;
            sensorComplete[1] = 0;
            sensorComplete[2] = 0;
            sensorComplete[3] = 0;
            sensorComplete[4] = 0;

//            Log.d(TAG, "test: X:" + accValue[0] + " Y: " + accValue[1] + " Z: " + accValue[2]);

            /* Calculating rotation matrix for orientation angle */
            SensorManager.getRotationMatrix(rotationMatrix, null, accValue, magValue);

            /* Getting orientation angle [deg or rad] */
            SensorManager.getOrientation(rotationMatrix, orientationAngles);
            sendingArray[0] = orientationAngles[0];      // Azimuth (Z-axis) - Yaw
            sendingArray[1] = orientationAngles[1];      // Pitch (X-axis)
            sendingArray[2] = orientationAngles[2];      // Roll (Y-axis)

//            Log.d(TAG, "test: X:" + sendingArray[9] + " Y: " + sendingArray[10] + " Z: " + sendingArray[11]);


            for(int eleCnt = 0; eleCnt < 12; eleCnt++)
            {
                dataToServer[dataCount*12 + eleCnt] = sendingArray[eleCnt];
            }
            dataCount++;

            if(dataCount == 128)
            {
                dataCount = 0;
                volleyPost(dataToServer);
            }


        }
    }


    protected void startMeasurement() {
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);

        /* Linear Acceleration sensor for measuring acceleration force in m/s^2 of the device,
           excluding the force of gravity */
        mAccLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        if (mAccLinear != null) {
            mSensorManager.registerListener(this, mAccLinear,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        /* Gravity sensor for measuring the force of gravity in m/s^2 */
        mGravity = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (mGravity != null) {
            mSensorManager.registerListener(this, mGravity,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        /* Gyroscope sensor for measuring rate of rotation in rad/s */
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (mGyro != null) {
            mSensorManager.registerListener(this, mGyro,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        /* Accelerometer sensor for measuring the acceleration force in m/s^2 of the device,
           including the force of gravity, used for calculating rotation matrix */
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAcc != null) {
            mSensorManager.registerListener(this, mAcc,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        /* Magnometer sensor for collecting additional data to calculate rotation matrix, that is
           used for calculating acceleration data to eliminate the force of gravity */
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (mMag != null) {
            mSensorManager.registerListener(this, mMag,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

    }


    private void stopMeasurement() {
        if (mSensorManager != null)
            mSensorManager.unregisterListener(this);

    }

}
