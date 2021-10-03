package com.mi_demo.mi02_processthread;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity implements Runnable{
    private final String data = "https://isis.pknu.ac.kr/gaon_big.jpg";

    private class ImageDownloadTask extends AsyncTask<String, Integer, Bitmap> {

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView iv  = (ImageView) findViewById(R.id.test);
            iv.setImageBitmap(bitmap);
            Toast.makeText(getBaseContext(), "Image Download!", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            Bitmap image = null;
            InputStream is = null;
            try {
                URL url = null;
                HttpsURLConnection conn =
                        (HttpsURLConnection) url.openConnection();
                conn.connect();;
                is = conn.getInputStream();
                image = BitmapFactory.decodeStream(is);

            } catch (IOException e) {
                Log.d("THREAD", "Download Error " + e.getMessage());
            }

            return image;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.runOnUiThread(this);
        Log.d("THREAD", "all done.");
    }

//    public void processClick(View v){
//        if (!running)
//            new Thread(this).start();
//    }

    @Override
    public void run() {
////        Log.d("THREAD", "onCreate(): " + Thread.currentThread().getName());
////        for(int i = 0; i < 5; i++){
////            try{
////                Thread.sleep(6000);
////                Log.d("Thread", "delays return:" + i);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////        }
////
////        Log.d("THREAD", "all done.");
//
//        running = true;
//
//        Button btn = (Button) findViewById(R.id.test);
//        // Using the POST method, the button post the action into the queue as an event and the queue will dispatch the action based on FIFO order
//        // Manipulating UI from external Thread (Event Driven)
//        btn.post(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("THREAD", "onCreate(): " + Thread.currentThread().getName());
//                btn.setText("Changed");
//            }
//        });
//
//
//        running = false;


        new ImageDownloadTask().execute(data);
    }
}