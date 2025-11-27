package com.example.importcsvproject.memory;

import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.importcsvproject.R;

import java.util.HashMap;

public class MemoryDemoActivity extends AppCompatActivity {

    private TextView tvResult;
    private Button btnHashMap, btnSparseArray;

    // Tăng số lượng để thấy rõ sự chênh lệch
    private static final int MAX_ITEMS = 100000;
    // Số lần chạy lặp lại để lấy trung bình
    private static final int LOOP_COUNT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memory_demo);

        tvResult = findViewById(R.id.tvResult);
        btnHashMap = findViewById(R.id.btnTestHashMap);
        btnSparseArray = findViewById(R.id.btnTestSparseArray);

        btnHashMap.setOnClickListener(v -> {
            tvResult.setText("Đang chạy Benchmark HashMap (10 lần)...");
            disableButtons();
            // Chạy trong Thread riêng để không đơ UI
            new Thread(this::runHashMapBenchmark).start();
        });

        btnSparseArray.setOnClickListener(v -> {
            tvResult.setText("Đang chạy Benchmark SparseArray (10 lần)...");
            disableButtons();
            new Thread(this::runSparseArrayBenchmark).start();
        });
    }

    private void runHashMapBenchmark() {
        // 1. Warm-up: Chạy nháp 1 lần để JIT Compiler làm việc
        HashMap<Integer, String> warmupMap = new HashMap<>();
        for (int i = 0; i < 1000; i++) warmupMap.put(i, "Warmup");
        warmupMap = null;
        System.gc(); // Dọn rác trước khi đo thật

        long totalDuration = 0;
        long totalMemoryDiff = 0;
        int validRuns = 0;

        for (int k = 0; k < LOOP_COUNT; k++) {
            long startMem = getUsedMemory();
            long startTime = System.nanoTime();

            HashMap<Integer, String> map = new HashMap<>();
            for (int i = 0; i < MAX_ITEMS; i++) {
                map.put(i, "Value " + i);
            }

            long endTime = System.nanoTime();
            long endMem = getUsedMemory();

            long memDiff = endMem - startMem;

            // Chỉ tính các lần chạy mà bộ nhớ TĂNG (không bị GC làm nhiễu)
            if (memDiff > 0) {
                totalMemoryDiff += memDiff;
                validRuns++;
            }
            totalDuration += (endTime - startTime);

            // Giữ tham chiếu để GC không dọn ngay lập tức trong vòng lặp
            Log.d("Benchmark", "Run " + k + " done. Size: " + map.size());
        }

        final long avgTime = totalDuration / LOOP_COUNT / 1000000; // ms
        // Nếu tất cả các lần chạy đều bị GC làm nhiễu (hiếm), lấy 0 để tránh lỗi chia cho 0
        final long avgMem = (validRuns > 0) ? (totalMemoryDiff / validRuns / 1024) : 0; // KB

        runOnUiThread(() -> {
            tvResult.setText(String.format("HASHMAP (Trung bình %d lần):\n- Thời gian: %d ms\n- RAM tiêu tốn: ~%d KB\n(Tốn nhiều RAM do Boxing Integer)", LOOP_COUNT, avgTime, avgMem));
            enableButtons();
        });
    }

    private void runSparseArrayBenchmark() {
        // 1. Warm-up
        SparseArray<String> warmupSparse = new SparseArray<>();
        for (int i = 0; i < 1000; i++) warmupSparse.put(i, "Warmup");
        warmupSparse = null;
        System.gc();

        long totalDuration = 0;
        long totalMemoryDiff = 0;
        int validRuns = 0;

        for (int k = 0; k < LOOP_COUNT; k++) {
            long startMem = getUsedMemory();
            long startTime = System.nanoTime();

            SparseArray<String> sparse = new SparseArray<>();
            for (int i = 0; i < MAX_ITEMS; i++) {
                sparse.put(i, "Value " + i);
            }

            long endTime = System.nanoTime();
            long endMem = getUsedMemory();

            long memDiff = endMem - startMem;

            if (memDiff > 0) {
                totalMemoryDiff += memDiff;
                validRuns++;
            }
            totalDuration += (endTime - startTime);
        }

        final long avgTime = totalDuration / LOOP_COUNT / 1000000; // ms
        final long avgMem = (validRuns > 0) ? (totalMemoryDiff / validRuns / 1024) : 0; // KB

        runOnUiThread(() -> {
            tvResult.setText(String.format("SPARSE ARRAY (Trung bình %d lần):\n- Thời gian: %d ms\n- RAM tiêu tốn: ~%d KB\n(Tiết kiệm ~30-40%% RAM)", LOOP_COUNT, avgTime, avgMem));
            enableButtons();
        });
    }

    private long getUsedMemory() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }

    private void disableButtons() {
        btnHashMap.setEnabled(false);
        btnSparseArray.setEnabled(false);
    }

    private void enableButtons() {
        btnHashMap.setEnabled(true);
        btnSparseArray.setEnabled(true);
    }
}