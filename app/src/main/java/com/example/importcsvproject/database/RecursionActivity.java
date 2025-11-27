package com.example.importcsvproject.database;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.importcsvproject.R;

public class RecursionActivity extends AppCompatActivity {

    private EditText etInputN;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recursion);

        etInputN = findViewById(R.id.etInputN);
        tvResult = findViewById(R.id.tvResult);

        findViewById(R.id.btnRunRecursive).setOnClickListener(v -> runRecursive());
        findViewById(R.id.btnRunIterative).setOnClickListener(v -> runIterative());
    }

    private void runRecursive() {
        try {
            int n = Integer.parseInt(etInputN.getText().toString());
            tvResult.setText("Đang chạy...");

            long startTime = System.currentTimeMillis();
            long sum = recursiveSum(n); // Có thể gây crash nếu n lớn
            long endTime = System.currentTimeMillis();

            tvResult.setText("Đệ quy thành công!\nKết quả: " + sum + "\nThời gian: " + (endTime - startTime) + " ms");
            tvResult.setTextColor(Color.BLACK);
        } catch (StackOverflowError e) {
            tvResult.setText("CRASH: StackOverflowError!\nNguyên nhân: Đệ quy quá sâu làm tràn bộ nhớ Stack.");
            tvResult.setTextColor(Color.RED);
        } catch (Exception e) {
            tvResult.setText("Lỗi nhập liệu");
        }
    }

    private void runIterative() {
        try {
            int n = Integer.parseInt(etInputN.getText().toString());

            long startTime = System.currentTimeMillis();
            long sum = 0;
            // Thuật toán Lặp (Khử đệ quy)
            for (int i = 1; i <= n; i++) {
                sum += i;
            }
            long endTime = System.currentTimeMillis();

            tvResult.setText("Lặp (Iterative) thành công!\nKết quả: " + sum + "\nThời gian: " + (endTime - startTime) + " ms");
            tvResult.setTextColor(Color.BLUE);
        } catch (Exception e) {
            tvResult.setText("Lỗi nhập liệu");
        }
    }

    // Hàm đệ quy sâu
    private long recursiveSum(int n) {
        if (n == 1) return 1;
        return n + recursiveSum(n - 1);
    }
}
