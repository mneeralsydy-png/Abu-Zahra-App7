package com.abualzahra.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {
    EditText etPhoneNumber;
    TextView tvBalance;
    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        tvBalance = findViewById(R.id.tvBalance);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        // تحميل الرصيد
        userRef.child("balance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    double bal = snapshot.getValue(Double.class);
                    tvBalance.setText("الرصيد: $" + String.format("%.2f", bal));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // طلب الأذونات
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
        }

        findViewById(R.id.btnCall).setOnClickListener(v -> startCall());
        findViewById(R.id.btnDelete).setOnClickListener(v -> {
            String text = etPhoneNumber.getText().toString();
            if (text.length() > 0) {
                etPhoneNumber.setText(text.substring(0, text.length() - 1));
                etPhoneNumber.setSelection(etPhoneNumber.getText().length());
            }
        });
    }

    public void onDialClick(View view) {
        android.widget.Button button = (android.widget.Button) view;
        String current = etPhoneNumber.getText().toString() + button.getText().toString();
        etPhoneNumber.setText(current);
        etPhoneNumber.setSelection(current.length());
    }

    private void startCall() {
        String number = etPhoneNumber.getText().toString().trim();
        if (number.isEmpty()) {
            Toast.makeText(this, "أدخل رقماً", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra("NUMBER", number);
        startActivity(intent);
    }
}
