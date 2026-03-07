package com.abualzahra.app;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.ConnectOptions;
import com.twilio.voice.Voice;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class CallActivity extends AppCompatActivity {
    TextView tvStatus, tvNumber;
    Call activeCall;
    
    // تم وضع الرابط الخاص بك هنا
    String SERVER_URL = "https://9424e054-f128-42e3-bff2-a475eb231a04-00-3fw9va4yton87.sisko.replit.dev/";
    String identity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        String number = getIntent().getStringExtra("NUMBER");
        tvNumber = findViewById(R.id.tvCallNumber);
        tvStatus = findViewById(R.id.tvCallStatus);
        
        tvNumber.setText(number);
        tvStatus.setText("جاري الاتصال...");

        identity = FirebaseAuth.getInstance().getCurrentUser().getUid();
        fetchTokenAndCall(number);
    }

    private void fetchTokenAndCall(String targetNumber) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        TokenService service = retrofit.create(TokenService.class);
        service.getToken(identity).enqueue(new Callback<TokenResponse>() {
            @Override
            public void onResponse(Call<TokenResponse> call, Response<TokenResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().token;
                    makeCall(token, targetNumber);
                } else {
                    tvStatus.setText("فشل الاتصال بالخادم");
                    Toast.makeText(CallActivity.this, "تأكد من أن الخادم يعمل ويدعم /token", Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<TokenResponse> call, Throwable t) {
                tvStatus.setText("خطأ في الشبكة: " + t.getMessage());
            }
        });
    }

    private void makeCall(String token, String number) {
        Map<String, String> params = new HashMap<>();
        params.put("To", number);

        ConnectOptions options = new ConnectOptions.Builder(token)
                .params(params)
                .build();

        activeCall = Voice.connect(CallActivity.this, options, new Call.Listener() {
            @Override
            public void onConnected(Call call) {
                tvStatus.setText("متصل");
            }

            @Override
            public void onDisconnected(Call call, CallException error) {
                tvStatus.setText("تم إنهاء المكالمة");
                finish();
            }

            @Override
            public void onConnectFailure(Call call, CallException error) {
                tvStatus.setText("فشل الاتصال: " + error.getMessage());
            }
        });
    }

    public void endCall(View view) {
        if (activeCall != null) {
            activeCall.disconnect();
        }
        finish();
    }

    // Interface for Retrofit
    public interface TokenService {
        @GET("token")
        Call<TokenResponse> getToken(@Query("identity") String identity);
    }

    public class TokenResponse {
        String token;
    }
}
