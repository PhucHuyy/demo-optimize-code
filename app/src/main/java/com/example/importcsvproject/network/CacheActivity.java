package com.example.importcsvproject.network;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.importcsvproject.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CacheActivity extends AppCompatActivity {

    private RecyclerView rvImages;
    private CheckBox cbUseCache;
    private Button btnClearCache;
    private ImageAdapter adapter;
    private LruCache<String, Bitmap> memoryCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache);

        // 1. Khởi tạo LruCache (1/8 RAM)
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8;
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };

        rvImages = findViewById(R.id.rvImages);
        cbUseCache = findViewById(R.id.cbUseCache);
        btnClearCache = findViewById(R.id.btnClearCache);

        rvImages.setLayoutManager(new LinearLayoutManager(this));

        // 2. Tạo danh sách URL ảnh thật
        List<String> urls = new ArrayList<>();
        for (int i = 10; i < 50; i++) {
            // Picsum photos: ảnh ngẫu nhiên kích thước 500x300
            urls.add("https://picsum.photos/500/300?random=" + i);
        }

        adapter = new ImageAdapter(urls);
        rvImages.setAdapter(adapter);

        btnClearCache.setOnClickListener(v -> {
            memoryCache.evictAll();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Đã xóa sạch Cache!", Toast.LENGTH_SHORT).show();
        });
    }

    // --- ADAPTER ---
    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private List<String> urls;

        public ImageAdapter(List<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String url = urls.get(position);
            holder.ivImage.setImageBitmap(null); // Reset ảnh cũ để tránh lỗi hiển thị sai
            holder.tvLoadTime.setText("Loading...");

            boolean useCache = cbUseCache.isChecked();
            long start = System.currentTimeMillis();

            // LOGIC QUAN TRỌNG: Kiểm tra Cache
            if (useCache) {
                Bitmap cachedBitmap = memoryCache.get(url);
                if (cachedBitmap != null) {
                    holder.ivImage.setImageBitmap(cachedBitmap);
                    holder.tvLoadTime.setText("Cache HIT: " + (System.currentTimeMillis() - start) + " ms");
                    return; // Xong, không cần tải mạng
                }
            }

            // Nếu không có Cache hoặc Cache Miss -> Tải Mạng
            new ImageLoaderTask(holder.ivImage, holder.tvLoadTime, url, useCache).execute();
        }

        @Override
        public int getItemCount() {
            return urls.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvLoadTime;
            ViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivItemImage);
                tvLoadTime = itemView.findViewById(R.id.tvLoadTime);
            }
        }
    }

    // --- ASYNC TASK ĐỂ TẢI ẢNH (THREAD RIÊNG) ---
    class ImageLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private ImageView imageView;
        private TextView tvTime;
        private String url;
        private boolean useCache;
        private long startTime;

        public ImageLoaderTask(ImageView imageView, TextView tvTime, String url, boolean useCache) {
            this.imageView = imageView;
            this.tvTime = tvTime;
            this.url = url;
            this.useCache = useCache;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        protected Bitmap doInBackground(Void... voids) {
            try {
                URL imageUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) imageUrl.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                // Lưu vào Cache nếu được phép
                if (useCache) {
                    memoryCache.put(url, result);
                }
                imageView.setImageBitmap(result);
                long time = System.currentTimeMillis() - startTime;
                tvTime.setText((useCache ? "Cache MISS (Net): " : "No Cache (Net): ") + time + " ms");
            }
        }
    }
}
