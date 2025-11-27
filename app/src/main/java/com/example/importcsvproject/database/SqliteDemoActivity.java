package com.example.importcsvproject.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.importcsvproject.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SqliteDemoActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private List<String[]> records;
    private TextView txtResult;
    private Spinner spinnerDataset;
    private ProgressBar progressBar;

    // Khai báo button để disable khi đang chạy
    private Button btnNaive, btnTrans, btnPrep, btnBatch;

    private final String[] DATA_OPTIONS = {"1000", "1500", "2000", "3000", "5000", "10000"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sqlite_demo);

        dbHelper = new DatabaseHelper(this);
        txtResult = findViewById(R.id.txtResult);
        spinnerDataset = findViewById(R.id.spinnerDataset);
        progressBar = findViewById(R.id.progressBar);

        btnNaive = findViewById(R.id.btnInsertNaive);
        btnTrans = findViewById(R.id.btnInsertTransaction);
        btnPrep = findViewById(R.id.btnInsertPrepared);
        btnBatch = findViewById(R.id.btnInsertBatch);

        setupSpinner();

        btnNaive.setOnClickListener(v -> runInsertNaive());
        btnTrans.setOnClickListener(v -> runInsertTransaction());
        btnPrep.setOnClickListener(v -> runInsertPrepared());
        btnBatch.setOnClickListener(v -> runInsertBatch());
    }

    // --- CÁC HÀM HỖ TRỢ ---

    private void setupSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, DATA_OPTIONS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDataset.setAdapter(adapter);

        spinnerDataset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String size = DATA_OPTIONS[position];
                String fileName = "sample_" + size + "_records.csv";

                // Load CSV trong Thread riêng để tránh đơ UI lúc khởi tạo
                toggleLoading(true);
                new Thread(() -> {
                    records = loadCSV(fileName);
                    runOnUiThread(() -> {
                        toggleLoading(false);
                        txtResult.setText("Đã sẵn sàng!\nDữ liệu: " + size + " dòng.\nRAM hiện tại: " + (getUsedMemory()/1024) + " KB");
                    });
                }).start();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void toggleLoading(boolean isLoading) {
        // Hiện/Ẩn ProgressBar và Khóa/Mở nút bấm
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnNaive.setEnabled(!isLoading);
        btnTrans.setEnabled(!isLoading);
        btnPrep.setEnabled(!isLoading);
        btnBatch.setEnabled(!isLoading);
        spinnerDataset.setEnabled(!isLoading);
    }

    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private List<String[]> loadCSV(String fileName) {
        List<String[]> list = new ArrayList<>();
        try {
            InputStream is = getAssets().open(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                list.add(line.split(","));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private void clearTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM users");
        db.close();
    }

    // --- CÁC HÀM BENCHMARK (Đã cập nhật đo RAM + Loading) ---

    private void runInsertNaive() {
        if (records == null || records.isEmpty()) return;

        toggleLoading(true); // Bắt đầu loading
        txtResult.setText("Đang chạy Naive Insert...");

        new Thread(() -> {
            clearTable(); // Xóa dữ liệu cũ
            System.gc();  // Gợi ý dọn rác để đo RAM chuẩn hơn

            long startMem = getUsedMemory();
            long startTime = System.currentTimeMillis();

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (String[] row : records) {
                ContentValues cv = new ContentValues();
                cv.put("name", row[0]);
                cv.put("username", row[1]);
                db.insert("users", null, cv);
            }
            db.close();

            long endTime = System.currentTimeMillis();
            long endMem = getUsedMemory();

            updateResultUI("Naive Insert", endTime - startTime, endMem - startMem);
        }).start();
    }

    private void runInsertTransaction() {
        if (records == null || records.isEmpty()) return;

        toggleLoading(true);
        txtResult.setText("Đang chạy Transaction...");

        new Thread(() -> {
            clearTable();
            System.gc();

            long startMem = getUsedMemory();
            long startTime = System.currentTimeMillis();

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (String[] row : records) {
                    ContentValues cv = new ContentValues();
                    cv.put("name", row[0]);
                    cv.put("username", row[1]);
                    db.insert("users", null, cv);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            db.close();

            long endTime = System.currentTimeMillis();
            long endMem = getUsedMemory();

            updateResultUI("Transaction", endTime - startTime, endMem - startMem);
        }).start();
    }

    private void runInsertPrepared() {
        if (records == null || records.isEmpty()) return;

        toggleLoading(true);
        txtResult.setText("Đang chạy Prepared Statement...");

        new Thread(() -> {
            clearTable();
            System.gc();

            long startMem = getUsedMemory();
            long startTime = System.currentTimeMillis();

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String sql = "INSERT INTO users(name, username) VALUES (?, ?)";
            SQLiteStatement stmt = db.compileStatement(sql);

            for (String[] row : records) {
                stmt.clearBindings();
                stmt.bindString(1, row[0]);
                stmt.bindString(2, row[1]);
                stmt.executeInsert();
            }
            db.close();

            long endTime = System.currentTimeMillis();
            long endMem = getUsedMemory();

            updateResultUI("Prepared Stmt", endTime - startTime, endMem - startMem);
        }).start();
    }

    private void runInsertBatch() {
        if (records == null || records.isEmpty()) return;

        toggleLoading(true);
        txtResult.setText("Đang chạy Batch Insert...");

        new Thread(() -> {
            clearTable();
            System.gc();

            long startMem = getUsedMemory();
            long startTime = System.currentTimeMillis();

            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String sql = "INSERT INTO users(name, username) VALUES (?, ?)";
            SQLiteStatement stmt = db.compileStatement(sql);

            db.beginTransaction();
            try {
                for (String[] row : records) {
                    stmt.clearBindings();
                    stmt.bindString(1, row[0]);
                    stmt.bindString(2, row[1]);
                    stmt.executeInsert();
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            db.close();

            long endTime = System.currentTimeMillis();
            long endMem = getUsedMemory();

            updateResultUI("Batch Insert", endTime - startTime, endMem - startMem);
        }).start();
    }

    // Hàm cập nhật kết quả chung cho tất cả
    private void updateResultUI(String method, long timeMs, long memBytes) {
        runOnUiThread(() -> {
            toggleLoading(false); // Tắt loading

            // Chuyển đổi byte sang KB hoặc MB
            String memStr;
            if (Math.abs(memBytes) > 1024 * 1024) {
                memStr = String.format("%.2f MB", memBytes / (1024.0 * 1024.0));
            } else {
                memStr = String.format("%d KB", memBytes / 1024);
            }

            txtResult.setText(String.format("KẾT QUẢ: %s\n\n- Thời gian: %d ms\n- RAM biến động: %s\n(Số lượng: %d dòng)",
                    method, timeMs, memStr, records.size()));
        });
    }
}