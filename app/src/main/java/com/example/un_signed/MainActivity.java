package com.example.un_signed;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button btnEnter = findViewById(R.id.btn_enter);
        new android.os.Handler().postDelayed(() -> {
            btnEnter.setVisibility(View.VISIBLE);
        }, 3000); // 3 seconds delay

        btnEnter.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileSelectionActivity.class);
            startActivity(intent);
        });
    }
}