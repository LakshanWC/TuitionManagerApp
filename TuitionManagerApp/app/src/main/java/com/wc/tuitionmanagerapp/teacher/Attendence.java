package com.wc.tuitionmanagerapp.teacher;

import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.wc.tuitionmanagerapp.R;

public class Attendence extends AppCompatActivity {

    private LinearLayout qrCard ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendence);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        qrCard = findViewById(R.id.qr_Card);
        Animation wiggleAnimation = AnimationUtils.loadAnimation(this,R.anim.wiggle);
        qrCard.startAnimation(wiggleAnimation);
    }

    public void scanQRCode(View view){
        IntentIntegrator myIntentIntegrator = new IntentIntegrator(Attendence.this);
        myIntentIntegrator.initiateScan();
    }
}