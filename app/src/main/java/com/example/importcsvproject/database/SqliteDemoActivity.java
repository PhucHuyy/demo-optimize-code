package com.example.importcsvproject.database;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.widget.TextView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sqlite_demo);

        dbHelper = new DatabaseHelper(this);
        txtResult = findViewById(R.id.txtResult);

        // Đọc CSV
        records = loadCSV();

        findViewById(R.id.btnInsertNaive).setOnClickListener(v -> runInsertNaive());
        findViewById(R.id.btnInsertTransaction).setOnClickListener(v -> runInsertTransaction());
        findViewById(R.id.btnInsertPrepared).setOnClickListener(v -> runInsertPrepared());
        findViewById(R.id.btnInsertBatch).setOnClickListener(v -> runInsertBatch());
    }

    private List<String[]> loadCSV() {
        List<String[]> list = new ArrayList<>();
        try {
            InputStream is = getAssets().open("sample_10000_records.csv");
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; } // bỏ header
                list.add(line.split(","));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return list;
    }

    private void clearTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM users");
        db.close();
    }

    private void runInsertNaive() {
        new Thread(() -> {
            clearTable();

            long start = System.currentTimeMillis();
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            for (String[] row : records) {
                ContentValues cv = new ContentValues();
                cv.put("name", row[0]);
                cv.put("username", row[1]);
                db.insert("users", null, cv);
            }

            db.close();
            long end = System.currentTimeMillis();

            runOnUiThread(() -> txtResult.setText("Naive: " + (end - start) + " ms"));
        }).start();
    }

    private void runInsertTransaction() {
        new Thread(() -> {
            clearTable();

            long start = System.currentTimeMillis();
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
            long end = System.currentTimeMillis();

            runOnUiThread(() ->
                    txtResult.setText("Transaction: " + (end - start) + " ms"));
        }).start();
    }

    private void runInsertPrepared() {
        new Thread(() -> {
            clearTable();

            long start = System.currentTimeMillis();
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
            long end = System.currentTimeMillis();

            runOnUiThread(() ->
                    txtResult.setText("Prepared: " + (end - start) + " ms"));
        }).start();
    }

    private void runInsertBatch() {
        new Thread(() -> {
            clearTable();

            long start = System.currentTimeMillis();
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
            long end = System.currentTimeMillis();

            runOnUiThread(() ->
                    txtResult.setText("Batch (Prepared + Tx): " + (end - start) + " ms"));
        }).start();
    }
}
