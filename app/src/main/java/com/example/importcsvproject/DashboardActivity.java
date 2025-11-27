//package com.example.importcsvproject;
//
//import android.content.Intent;
//import android.os.Bundle;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//public class DashboardActivity extends AppCompatActivity {
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_dashboard);
//
//        findViewById(R.id.btnSqlite).setOnClickListener(v -> {
//            startActivity(new Intent(this, SqliteDemoActivity.class));
//        });
//
//        findViewById(R.id.btnMemory).setOnClickListener(v -> {
//            startActivity(new Intent(this, MemoryDemoActivity.class));
//        });
//
//        findViewById(R.id.btnKotlin).setOnClickListener(v -> {
//            startActivity(new Intent(this, KotlinFeaturesActivity.class));
//        });
//    }
//}
package com.example.importcsvproject;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.importcsvproject.database.RecursionActivity;
import com.example.importcsvproject.database.SqliteDemoActivity;
import com.example.importcsvproject.memory.BitmapActivity;
import com.example.importcsvproject.memory.BitmapDownsamplingActivity;
import com.example.importcsvproject.memory.MemoryDemoActivity;
import com.example.importcsvproject.network.CacheActivity;
import com.example.importcsvproject.network.KotlinFeaturesActivity;

public class DashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // --- NHÓM 1: Project Cũ ---
        findViewById(R.id.btnSqlite).setOnClickListener(v ->
                startActivity(new Intent(this, SqliteDemoActivity.class)));

        findViewById(R.id.btnMemory).setOnClickListener(v ->
                startActivity(new Intent(this, MemoryDemoActivity.class)));

        findViewById(R.id.btnKotlin).setOnClickListener(v ->
                startActivity(new Intent(this, KotlinFeaturesActivity.class)));

        // --- NHÓM 2: Project Mới (Vừa thêm vào) ---

        // 1. Đệ quy
        findViewById(R.id.btnRecursion).setOnClickListener(v ->
                startActivity(new Intent(this, RecursionActivity.class)));

        // 2. Bitmap Memory (Config RGB_565)
        findViewById(R.id.btnBitmapMem).setOnClickListener(v ->
                startActivity(new Intent(this, BitmapActivity.class)));

        // 3. Bitmap Downsampling (Load ảnh to)
        findViewById(R.id.btnBitmapDownsample).setOnClickListener(v ->
                startActivity(new Intent(this, BitmapDownsamplingActivity.class)));

        // 4. Cache Ảnh
        findViewById(R.id.btnCache).setOnClickListener(v ->
                startActivity(new Intent(this, CacheActivity.class)));
    }
}