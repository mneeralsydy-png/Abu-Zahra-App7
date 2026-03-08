package com.abualzahra.app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.ConnectOptions;
import com.twilio.voice.Voice;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class CallActivity extends AppCompatActivity {

    // عناصر الواجهة
    private TextView tvCallerName, tvCallNumber, tvStatus, tvTimer;
    private ImageButton btnMute, btnSpeaker;
    
    // متغيرات المكالمة
    private Call activeCall;
    private String phoneNumber;
    private String accessToken;
    
    // متغيرات المؤقت
    private Handler timerHandler = new Handler();
    private int seconds = 0;

    // رابط الخادم (تأكد من صحته)
    private String SERVER_URL = "https://9424e054-f128-42e3-bff2-a475eb231a04-00-3fw9va4yton87.sisko.replit.dev/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        // ربط العناصر مع الواجهة
        tvCallerName = findViewById(R.id.tvCallerName);
        tvCallNumber = findViewById(R.id.tvCallNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvTimer = findViewById(R.id.tvTimer);
        btnMute = findViewById(R.id.btnMute);
        btnSpeaker = findViewById(R.id.btnSpeaker);

        // الحصول على الرقم من الشاشة السابقة
        phoneNumber = getIntent().getStringExtra("NUMBER");
        if (phoneNumber != null) {
            tvCallNumber.setText(phoneNumber);
        }

        // طلب إذن الميكروفون إذا لم يكن موجوداً
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 101);
        }

        // بدء عملية الاتصال
        fetchTokenAndConnect();
    }

    // --- 1. جلب التوكن من الخادم ---
    private void fetchTokenAndConnect() {
        tvStatus.setText("جاري الاتصال...");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TokenService service = retrofit.create(TokenService.class);
        String identity = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // استخدام retrofit2.Call لتفادي التعارض مع Twilio Call
        service.getToken(identity).enqueue(new retrofit2.Callback<TokenResponse>() {
            @Override
            public void onResponse(retrofit2.Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    accessToken = response.body().token;
                    connectToPhone(); // بدء الاتصال بعد الحصول على التوكن
                } else {
                    tvStatus.setText("فشل الاتصال بالخادم");
                    Toast.makeText(CallActivity.this, "خطأ في الخادم: تأكد من تشغيل Replit", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<TokenResponse> call, Throwable t) {
                tvStatus.setText("خطأ في الشبكة");
                Toast.makeText(CallActivity.this, "خطأ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- 2. بدء الاتصال باستخدام Twilio ---
    private void connectToPhone() {
        if (accessToken == null) return;

        Map<String, String> params = new HashMap<>();
        params.put("To", phoneNumber); // الرقم المستهدف

        ConnectOptions options = new ConnectOptions.Builder(accessToken)
                .params(params)
                .build();

        activeCall = Voice.connect(CallActivity.this, options, new Call.Listener() {
            @Override
            public void onConnected(Call call) {
                tvStatus.setText("متصل");
                startTimer(); // بدء المؤقت
            }

            @Override
            public void onDisconnected(Call call, CallException error) {
                tvStatus.setText("تم إنهاء المكالمة");
                stopTimer();
                finish(); // إغلاق الشاشة
            }

            @Override
            public void onConnectFailure(Call call, CallException error) {
                tvStatus.setText("فشل الاتصال");
                Toast.makeText(CallActivity.this, "خطأ: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                stopTimer();
            }

            @Override
            public void onRinging(Call call) {
                tvStatus.setText("يرن...");
            }

            @Override
            public void onReconnecting(Call call, CallException error) {
                tvStatus.setText("إعادة الاتصال...");
            }

            @Override
            public void onReconnected(Call call) {
                tvStatus.setText("متصل");
            }
        });
    }

    // --- 3. أزرار التحكم ---

    // زر كتم الصوت
    public void toggleMute(View view) {
        if (activeCall != null) {
            boolean muted = !activeCall.isMuted();
            activeCall.mute(muted);
            
            if (muted) {
                btnMute.setBackgroundResource(R.drawable.bg_key_circle);
                btnMute.setBackgroundResource(R.drawable.bg_key_circle); // اجعل الخلفية ملونة للدلالة على الكتم
                btnMute.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
                btnMute.setColorFilter(ContextCompat.getColor(this, R.color.white));
                Toast.makeText(this, "تم كتم الصوت", Toast.LENGTH_SHORT).show();
            } else {
                btnMute.setBackgroundResource(R.drawable.bg_key_circle);
                btnMute.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white)); // إعادة اللون الطبيعي
                btnMute.setColorFilter(ContextCompat.getColor(this, R.color.white));
                Toast.makeText(this, "تم إلغاء الكتم", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // زر مكبر الصوت
    public void toggleSpeaker(View view) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            boolean isSpeakerOn = audioManager.getMode() == AudioManager.MODE_IN_CALL; 
            // التبديل بين السماعة ومكبر الصوت
                if (audioManager.isSpeakerphoneOn()) { 
                audioManager.setSpeakerphoneOn(false);
                btnSpeaker.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.white));
                Toast.makeText(this, "السماعة العادية", Toast.LENGTH_SHORT).show();
            } else {
                audioManager.setSpeakerphoneOn(true);
                btnSpeaker.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.colorPrimary));
                Toast.makeText(this, "مكبر الصوت", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // زر إنهاء المكالمة
    public void hangupCall(View view) {
        if (activeCall != null) {
            activeCall.disconnect();
        }
        stopTimer();
        finish();
    }

    // --- 4. أدوات المؤقت ---
    private void startTimer() {
        seconds = 0;
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int mins = seconds / 60;
                int secs = seconds % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                seconds++;
                timerHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void stopTimer() {
        timerHandler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onDestroy() {
        stopTimer();
        if (activeCall != null) {
            activeCall.disconnect();
        }
        super.onDestroy();
    }

    // --- واجهة Retrofit ---
    public interface TokenService {
        @GET("token")
        retrofit2.Call<TokenResponse> getToken(@Query("identity") String identity);
    }

    public class TokenResponse {
        String token;
    }
}
