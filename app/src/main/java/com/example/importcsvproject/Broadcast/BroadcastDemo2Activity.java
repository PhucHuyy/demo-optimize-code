package com.example.importcsvproject.Broadcast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.importcsvproject.R;

public class BroadcastDemo2Activity extends AppCompatActivity {

    private static final String TAG = "OTP_DEMO";
    private static final String ACTION_FAKE_OTP = "com.example.ACTION_FAKE_OTP";

    private static final String KEY_OPTIMIZED = "key_optimized";
    private static final String KEY_COUNT     = "key_count";

    private TextView tvTitle, tvMode, tvOtp, tvCount, tvHint;
    private Button btnToggleMode, btnSendOtp, btnReset;
    private TextView tvOtpReceived;

    // Đếm số lần receiver xử lý OTP (static để thấy hiệu ứng leak)
    private static int count = 0;

    // Cờ: đang ở mode tối ưu hay chưa
    private boolean isOptimized = false;

    private BroadcastReceiver otpReceiver;
    private boolean isReceiverRegistered = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_demo2);

        tvTitle       = findViewById(R.id.tvTitle);
        tvMode        = findViewById(R.id.tvMode);
        tvCount       = findViewById(R.id.tvCount);
        btnToggleMode = findViewById(R.id.btnToggleMode);
        btnSendOtp    = findViewById(R.id.btnSendOtp);
        btnReset      = findViewById(R.id.btnReset);
        tvOtpReceived = findViewById(R.id.tvOtpReceived);


        // Khôi phục trạng thái khi xoay màn hình
        if (savedInstanceState != null) {
            isOptimized = savedInstanceState.getBoolean(KEY_OPTIMIZED, false);
            count       = savedInstanceState.getInt(KEY_COUNT, 0);
        }

        setupReceiver();

        updateModeText();
        updateCountText();


        // Chỉ ĐỔI CỜ, không register/unregister trong onClick nữa
        btnToggleMode.setOnClickListener(v -> {
            isOptimized = !isOptimized;

            // Đổi mode → reset count cho dễ nhìn
            count = 0;
            updateModeText();
            updateCountText();

            Toast.makeText(this,
                    "Đã chuyển sang mode " + (isOptimized ? "TỐI ƯU" : "CHƯA TỐI ƯU"),
                    Toast.LENGTH_SHORT).show();
        });

        // Gửi OTP giả
        btnSendOtp.setOnClickListener(v -> {
            // Clear log cũ để thấy rõ từng lượt broadcast
            tvOtpReceived.setText("Log Receiver:\n");

            // Random OTP 6 số
            int otp = 100000 + new java.util.Random().nextInt(900000);

            Intent i = new Intent(ACTION_FAKE_OTP);
            i.setPackage(getPackageName());
            i.putExtra("otp", String.valueOf(otp));
            sendBroadcast(i);
        });

        // Reset count
        btnReset.setOnClickListener(v -> {
            count = 0;
            tvCount.setText("Số lần receiver xử lý OTP: 0");
            tvOtpReceived.setText("Log Receiver:\n");
        });
    }

    private void updateModeText() {
        String modeText = isOptimized
                ? "Chế độ hiện tại: TỐI ƯU (register/unregister đúng vòng đời)"
                : "Chế độ hiện tại: CHƯA TỐI ƯU (đăng ký nhưng KHÔNG hủy)";
        tvMode.setText(modeText);
        btnToggleMode.setText(isOptimized ? "Tối ưu: ON" : "Tối ưu: OFF");
    }

    private void updateCountText() {
        tvCount.setText("Số lần receiver xử lý OTP: " + count);
    }

    private void setupReceiver() {
        otpReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String otp = intent.getStringExtra("otp");

                count++;

                tvCount.setText("Số lần receiver xử lý OTP: " + count);

                // Mỗi receiver in ra 1 dòng riêng
                String oldText = tvOtpReceived.getText().toString();

                String newLine = "Receiver " + this.hashCode()
                        + " nhận OTP: " + otp + "\n";

                tvOtpReceived.setText(oldText + newLine);

                Log.e("OTP_DEMO",
                        "Receiver=" + this.hashCode() +
                                " | OTP=" + otp +
                                " | count=" + count);

                // Log RAM sau mỗi lần xử lý OTP
                logMemory("onReceive - receiver=" + this.hashCode());
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

        // DÙ MODE NÀO cũng register trong onStart
        if (!isReceiverRegistered) {
            registerMyReceiver();
            Log.e(TAG, "onStart → registerMyReceiver()");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isOptimized) {
            // MODE TỐI ƯU: rời Activity là hủy đăng ký → KHÔNG leak
            if (isReceiverRegistered) {
                unregisterMyReceiver();
                Log.e(TAG, "onStop → unregisterMyReceiver()");
            }
        } else {
            // MODE CHƯA TỐI ƯU: CỐ TÌNH KHÔNG unregister để demo leak
            //Log.e(TAG, "onStop → KHÔNG unregister (MODE CHƯA TỐI ƯU) → có nguy cơ leak");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_OPTIMIZED, isOptimized);
        outState.putInt(KEY_COUNT, count);
    }

    // ==== REGISTER / UNREGISTER DYNAMIC RECEIVER ====
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerMyReceiver() {
        if (isReceiverRegistered) return;

        IntentFilter filter = new IntentFilter(ACTION_FAKE_OTP);

        if (Build.VERSION.SDK_INT >= 34) {
            registerReceiver(otpReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(otpReceiver, filter);
        }

        isReceiverRegistered = true;
        Log.e(TAG, "registerMyReceiver() | receiver=" + otpReceiver.hashCode());
    }

    private void unregisterMyReceiver() {
        if (!isReceiverRegistered) return;

        try {
            unregisterReceiver(otpReceiver);
        } catch (Exception ignored) {}

        isReceiverRegistered = false;
        Log.e(TAG, "unregisterMyReceiver() | receiver=" + otpReceiver.hashCode());
    }

    // ==== HÀM LOG RAM ====
    private void logMemory(String where) {
        Runtime rt = Runtime.getRuntime();
        long usedKb = (rt.totalMemory() - rt.freeMemory()) / 1024;
        long maxKb  = rt.maxMemory() / 1024;
        Log.e(TAG, where + " | heapUsed=" + usedKb + " KB / max=" + maxKb + " KB");
    }
}
