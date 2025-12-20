package com.example.importcsvproject.database;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.importcsvproject.R;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueryOptimizationActivity extends AppCompatActivity {

    private TextView tvResultNoOpt, tvResultOpt, tvResultProjection;
    private Button btnNoOpt, btnOpt, btnProjection;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_opt);

        tvResultNoOpt = findViewById(R.id.tvResultNoOpt);
        tvResultOpt = findViewById(R.id.tvResultOpt);
        btnNoOpt = findViewById(R.id.btnNoOpt);
        btnOpt = findViewById(R.id.btnOpt);

        tvResultProjection = findViewById(R.id.tvResultProjection);
        btnProjection = findViewById(R.id.btnProjection);

        btnNoOpt.setOnClickListener(v -> runQuery(1));
        btnOpt.setOnClickListener(v -> runQuery(2));
        btnProjection.setOnClickListener(v -> runQuery(3));
    }

    private void runQuery(int queryMode) {
        // 1. Xác định TextView đích dựa trên mode
        TextView targetView;
        if (queryMode == 1) targetView = tvResultNoOpt;
        else if (queryMode == 2) targetView = tvResultOpt;
        else targetView = tvResultProjection;

        targetView.setText("Đang chạy Benchmark (50 lần)...");

        // Disable tất cả các nút
        setButtonsEnabled(false);

        executorService.execute(() -> {
            Connection conn = MariaDbHelper.getConnection();
            if (conn == null) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "Lỗi kết nối Server!", Toast.LENGTH_SHORT).show();
                    setButtonsEnabled(true);
                });
                return;
            }

            String sql = "";
            String description = "";
            String param = "3.46.15.022.VIE.00.D10";

            switch (queryMode) {
                case 1:
                    sql = "SELECT * FROM product_new WHERE code = ?";
                    description = "Full Table Scan (code)";
                    break;
                case 2:
                    sql = "SELECT * FROM product_new WHERE ref_code = ?";
                    description = "Index Scan (ref_code) - Select All";
                    break;
                case 3:
                    sql = "SELECT id, name FROM product_new WHERE ref_code = ?";
                    description = "Index + Projection (Chỉ lấy id, name)";
                    break;
            }

            try {
                PreparedStatement stmtWarmup = conn.prepareStatement(sql);
                stmtWarmup.setString(1, param);
                for (int i = 0; i < 5; i++) {
                    ResultSet rs = stmtWarmup.executeQuery();
                    while (rs.next()) {}
                    rs.close();
                }
                stmtWarmup.close();

                int loopCount = 50;
                long totalTimeNs = 0;
                boolean found = false;
                String resultDetails = "";

                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, param);

                for (int i = 0; i < loopCount; i++) {
                    long start = System.nanoTime();
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        found = true;
                        if (i == loopCount - 1) {
                            resultDetails = "ID: " + rs.getString("id");
                        }
                    }
                    rs.close();
                    long end = System.nanoTime();
                    totalTimeNs += (end - start);
                }
                stmt.close();
                conn.close();

                double avgTimeMs = (totalTimeNs / (double) loopCount) / 1_000_000.0;
                boolean finalFound = found;
                String finalDetails = resultDetails;
                String finalDesc = description;

                // Update UI
                mainHandler.post(() -> {
                    String displayText = "Chiến thuật: " + finalDesc + "\n" +
                            "Trạng thái: " + (finalFound ? "TÌM THẤY" : "KHÔNG THẤY") + "\n" +
                            "Thời gian TB: " + String.format("%.4f", avgTimeMs) + " ms\n" +
                            "Data mẫu: " + finalDetails;

                    targetView.setText(displayText);
                    setButtonsEnabled(true);
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    targetView.setText("Lỗi: " + e.getMessage());
                    setButtonsEnabled(true);
                });
            }
        });
    }

    // Helper để bật tắt nút nhanh gọn
    private void setButtonsEnabled(boolean enabled) {
        runOnUiThread(() -> {
            btnNoOpt.setEnabled(enabled);
            btnOpt.setEnabled(enabled);
            btnProjection.setEnabled(enabled);
        });
    }
//private void runQuery(boolean isOptimized) {
//    TextView targetView = isOptimized ? tvResultOpt : tvResultNoOpt;
//    targetView.setText("Đang chạy Benchmark (100 lần)... vui lòng chờ!");
//
//    // Vô hiệu hóa nút bấm để tránh spam
//    findViewById(R.id.btnNoOpt).setEnabled(false);
//    findViewById(R.id.btnOpt).setEnabled(false);
//
//    executorService.execute(() -> {
//        Connection conn = MariaDbHelper.getConnection();
//        if (conn == null) {
//            mainHandler.post(() -> {
//                Toast.makeText(this, "Lỗi kết nối!", Toast.LENGTH_SHORT).show();
//                // Mở lại nút
//                findViewById(R.id.btnNoOpt).setEnabled(true);
//                findViewById(R.id.btnOpt).setEnabled(true);
//            });
//            return;
//        }
//
//        String sql;
//        String description;
//        String param = "3.46.15.022.VIE.00.D10";
//
//        // Cấu hình query
//        if (isOptimized) {
//            sql = "SELECT id, name, type FROM product_new WHERE ref_code = ?";
//            description = "Optimized (Index + Projection)";
//        } else {
//            sql = "SELECT * FROM product_new WHERE ref_code = ?";
//            description = "Slow (Full Select *)";
//        }
//
//        try {
//            // --- BƯỚC 1: WARM-UP (Làm nóng máy) ---
//            // Chạy nháp 5 lần đầu để DB nạp cache và JIT biên dịch code
//            // Kết quả này KHÔNG tính vào trung bình
//            PreparedStatement stmtWarmup = conn.prepareStatement(sql);
//            stmtWarmup.setString(1, param);
//            for (int i = 0; i < 5; i++) {
//                ResultSet rs = stmtWarmup.executeQuery();
//                while(rs.next()) {}; // Đọc giả
//                rs.close();
//            }
//            stmtWarmup.close();
//
//            // --- BƯỚC 2: BENCHMARK (Đo thật) ---
//            int loopCount = 50; // Chạy 50 lần lấy trung bình
//            long totalTimeNs = 0; // Tổng thời gian (Nano giây)
//
//            PreparedStatement stmt = conn.prepareStatement(sql);
//            stmt.setString(1, param);
//
//            for (int i = 0; i < loopCount; i++) {
//                long start = System.nanoTime();
//
//                ResultSet rs = stmt.executeQuery();
//                while(rs.next()) {
//                    // Giả lập đọc dữ liệu để tốn chút CPU
//                    String temp = rs.getString(1);
//                }
//                rs.close();
//
//                long end = System.nanoTime();
//                totalTimeNs += (end - start);
//            }
//            stmt.close();
//            conn.close();
//
//            // Tính trung bình
//            double avgTimeMs = (totalTimeNs / (double) loopCount) / 1_000_000.0;
//
//            // --- HIỂN THỊ KẾT QUẢ ---
//            mainHandler.post(() -> {
//                targetView.setText("Chiến thuật: " + description + "\n" +
//                        "Số lần chạy: " + loopCount + "\n" +
//                        "Trung bình: " + String.format("%.3f", avgTimeMs) + " ms\n" +
//                        "Kết luận: Ổn định");
//
//                // Mở lại nút
//                findViewById(R.id.btnNoOpt).setEnabled(true);
//                findViewById(R.id.btnOpt).setEnabled(true);
//            });
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    });
//}
}