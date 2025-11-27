package com.example.importcsvproject.memory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.importcsvproject.R;

public class BitmapActivity extends AppCompatActivity {

    private ImageView ivImage;
    private TextView tvMemInfo;
    private Bitmap currentBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap);

        ivImage = findViewById(R.id.ivImage);
        tvMemInfo = findViewById(R.id.tvMemInfo);

        // Lưu ý: Cần có ảnh res/drawable/test_image (Ảnh càng lớn càng rõ)
        // Nếu chưa có, hãy copy 1 ảnh vào res/drawable
        int resId = R.drawable.img_1; // Thay bằng R.drawable.test_image

        findViewById(R.id.btnLoadDefault).setOnClickListener(v -> loadBitmap(resId, false));
        findViewById(R.id.btnLoadOptimized).setOnClickListener(v -> loadBitmap(resId, true));
    }

    private void loadBitmap(int resId, boolean optimized) {
        // Giải phóng ảnh cũ để đo chính xác
        if (currentBitmap != null) {
            currentBitmap.recycle();
            currentBitmap = null;
            System.gc(); // Gợi ý dọn rác
        }

        BitmapFactory.Options options = new BitmapFactory.Options();

        if (optimized) {
            // Cấu hình tối ưu: 2 byte/pixel, không kênh Alpha
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        } else {
            // Cấu hình mặc định: 4 byte/pixel
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        }

        long startTime = System.currentTimeMillis();
        currentBitmap = BitmapFactory.decodeResource(getResources(), resId, options);
        long endTime = System.currentTimeMillis();

        ivImage.setImageBitmap(currentBitmap);

        // Tính dung lượng RAM thực tế
        int byteCount = currentBitmap.getAllocationByteCount();
        double mbCount = byteCount / (1024.0 * 1024.0);

        String configName = optimized ? "RGB_565 (Tối ưu)" : "ARGB_8888 (Gốc)";
        tvMemInfo.setText(String.format("Mode: %s\nRAM: %.2f MB\nThời gian decode: %d ms",
                configName, mbCount, (endTime - startTime)));
    }
}