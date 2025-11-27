package com.example.importcsvproject.memory;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.importcsvproject.R;

import java.util.ArrayList;
import java.util.List;

public class BitmapDownsamplingActivity extends AppCompatActivity {

    private LinearLayout containerImages;
    private TextView tvInfo;
    private List<Bitmap> loadedBitmaps = new ArrayList<>(); // Giữ tham chiếu để GC không dọn, nhằm đo RAM

    // Cấu hình kích thước thumbnail mong muốn (nhỏ hơn nhiều so với ảnh gốc)
    private static final int REQ_WIDTH = 300;
    private static final int REQ_HEIGHT = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap_downsampling);

        containerImages = findViewById(R.id.containerImages);
        tvInfo = findViewById(R.id.tvInfo);
        Button btnOriginal = findViewById(R.id.btnLoadOriginal);
        Button btnOptimized = findViewById(R.id.btnLoadOptimized);

        // 1. Nút Load Nguyên Bản (Nguy hiểm)
        btnOriginal.setOnClickListener(v -> {
            clearPreviousImages();
            tvInfo.setText("Đang load 10 ảnh gốc (Gây lag)...");

            // Chạy thread riêng để tránh đơ UI ngay lập tức
            new Thread(() -> {
                try {
                    long totalBytes = 0;
                    for (int i = 0; i < 10; i++) {
                        // LOAD THÔ: Đọc nguyên file ảnh lớn vào RAM
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.avatar);

                        if (bitmap != null) {
                            loadedBitmaps.add(bitmap);
                            totalBytes += bitmap.getAllocationByteCount();

                            // Cập nhật UI từng ảnh một
                            final int index = i;
                            final Bitmap finalBm = bitmap;
                            new Handler(Looper.getMainLooper()).post(() -> addImageToLayout(finalBm, "Ảnh gốc " + (index+1)));
                        }
                    }
                    updateMemoryInfo(totalBytes);
                } catch (OutOfMemoryError e) {
                    // Đây là điều chúng ta mong đợi khi load ảnh gốc
                    new Handler(Looper.getMainLooper()).post(() -> {
                        tvInfo.setText("CRASHED! Hết bộ nhớ (OOM)!");
                        Toast.makeText(BitmapDownsamplingActivity.this, "Điện thoại đã hết RAM!", Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
        });

        // 2. Nút Load Tối Ưu (An toàn)
        btnOptimized.setOnClickListener(v -> {
            clearPreviousImages();
            tvInfo.setText("Đang xử lý tối ưu...");

            new Thread(() -> {
                long totalBytes = 0;
                for (int i = 0; i < 10; i++) {
                    // LOAD TỐI ƯU: Tính toán và chỉ load bản thu nhỏ
                    Bitmap bitmap = decodeSampledBitmapFromResource(getResources(), R.drawable.img, REQ_WIDTH, REQ_HEIGHT);

                    if (bitmap != null) {
                        loadedBitmaps.add(bitmap);
                        totalBytes += bitmap.getAllocationByteCount();

                        final int index = i;
                        final Bitmap finalBm = bitmap;
                        new Handler(Looper.getMainLooper()).post(() -> addImageToLayout(finalBm, "Đã tối ưu " + (index+1)));
                    }
                }
                updateMemoryInfo(totalBytes);
            }).start();
        });
    }

    // --- HÀM HỖ TRỢ HIỂN THỊ ---
    private void addImageToLayout(Bitmap bitmap, String label) {
        ImageView iv = new ImageView(this);
        iv.setImageBitmap(bitmap);

        // Set cứng kích thước khung hiển thị (Thumbnail View)
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(REQ_WIDTH, REQ_HEIGHT);
        params.setMargins(0, 10, 0, 30);
        iv.setLayoutParams(params);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setGravity(Gravity.CENTER);

        containerImages.addView(tv);
        containerImages.addView(iv);
    }

    private void updateMemoryInfo(long totalBytes) {
        double mb = totalBytes / (1024.0 * 1024.0);
        String msg = String.format("Đã load 10 ảnh.\nTổng RAM tiêu thụ: %.2f MB", mb);

        new Handler(Looper.getMainLooper()).post(() -> tvInfo.setText(msg));
    }

    private void clearPreviousImages() {
        containerImages.removeAllViews();
        // Giải phóng bitmap cũ để trả lại RAM cho lần test sau
        for (Bitmap bm : loadedBitmaps) {
            if (!bm.isRecycled()) bm.recycle();
        }
        loadedBitmaps.clear();
        System.gc(); // Gợi ý hệ thống dọn rác ngay
    }

    // --- CỐT LÕI CỦA TỐI ƯU (Copy từ Google Docs) ---
    public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // Bước 1: Chỉ đọc kích thước ảnh (inJustDecodeBounds = true) chứ chưa load nội dung vào RAM
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Bước 2: Tính toán tỷ lệ inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Bước 3: Đọc ảnh thật với tỷ lệ đã tính (inJustDecodeBounds = false)
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Kích thước gốc của ảnh
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Tính lũy thừa của 2 sao cho kích thước vẫn lớn hơn kích thước yêu cầu
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}
