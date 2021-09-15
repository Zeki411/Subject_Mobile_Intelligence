package com.example.assignment_01;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private Button btnShowToast;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView view = new ImageView(MainActivity.this);
        view.setImageResource(R.drawable.satisfied);
        btnShowToast = (Button)this.findViewById(R.id.btn_showToast);
        btnShowToast.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater inflater = getLayoutInflater();

                // inflate layout file in Layout Inflater
                View view = inflater.inflate(R.layout.toast_image_custom,
                        (ViewGroup) findViewById(R.id.relativeLayout1));
                Toast topToast = new Toast(getApplicationContext());
                topToast.setView(view);
                topToast.setGravity(Gravity.TOP|Gravity.CENTER_HORIZONTAL,0,300);
                topToast.show();
            }
        });
    }


}