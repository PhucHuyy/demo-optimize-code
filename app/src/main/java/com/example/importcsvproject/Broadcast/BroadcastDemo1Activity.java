package com.example.importcsvproject.Broadcast;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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

public class BroadcastDemo1Activity extends AppCompatActivity {

    private static final String TAG = "DEMO";

    private RecyclerView rvImages;
    private CheckBox cbHeavyOnReceive;
    private Button btnReload;
    private TextView tvNetworkStatus;

    private ImageAdapter adapter;

    private BroadcastReceiver networkReceiver;
    private boolean isReceiverRegistered = false;

    private final List<String> urls = new ArrayList<>();

    // Lưu lại những ảnh đã load được (bitmap đã tải thành công)
    private final List<Bitmap> loadedBitmaps = new ArrayList<>();

    // Cờ trạng thái offline để onBindViewHolder biết có nên load nữa không
    private volatile boolean isOffline = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast_demo1);

        rvImages         = findViewById(R.id.rvImages);
        cbHeavyOnReceive = findViewById(R.id.cbHeavyOnReceive);
        btnReload        = findViewById(R.id.btnClearCache);
        tvNetworkStatus  = findViewById(R.id.tvNetworkStatus);

        // 1. Danh sách URL ảnh thật
        for (int i = 10; i < 50; i++) {
            urls.add("https://picsum.photos/500/300?random=" + i);
        }

        rvImages.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ImageAdapter(urls);
        rvImages.setAdapter(adapter);

        // Reload lại list
        btnReload.setText("Reload");
        btnReload.setOnClickListener(v -> {
            loadedBitmaps.clear(); // clear danh sách bitmap đã load
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Reload image list", Toast.LENGTH_SHORT).show();
        });

        // Setup BroadcastReceiver
        setupNetworkReceiver();

        boolean connected = checkNetworkConnected(this);
        isOffline = !connected;
        updateNetworkStatus(connected);
    }

    // ==========================================
    // BROADCAST: thay đổi mạng
    // ==========================================
    private void setupNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                long start = System.currentTimeMillis();
                boolean connected = checkNetworkConnected(context);
                isOffline = !connected; // cập nhật cờ cho adapter biết

                // MODE CHƯA TỐI ƯU – xử lý NẶNG ngay trong onReceive (UI thread)
                if (cbHeavyOnReceive.isChecked() && !connected) {
                    long heavyStart = System.currentTimeMillis();

                    int dummy = 0;
                    // duyệt qua hết bitmap đã tải, làm vài phép tính cho nặng CPU
                    for (Bitmap bmp : loadedBitmaps) {
                        if (bmp != null && !bmp.isRecycled()) {
                            dummy += bmp.getWidth() * bmp.getHeight();
                        }
                    }

                    // Giả lập thêm delay
                    try {
                        Thread.sleep(4000); // block main thread
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    long heavyCost = System.currentTimeMillis() - heavyStart;

                    // Cập nhật lại list
                    adapter.notifyDataSetChanged();

//                    String msgToast = "CHƯA TỐI ƯU: xử lý "
//                            + loadedBitmaps.size()
//                            + " ảnh trên UI thread ~ " + heavyCost + " ms";
//                    Toast.makeText(context, msgToast, Toast.LENGTH_LONG).show();

                    //  Logcat cho phần CHƯA TỐI ƯU
                    Log.e(TAG, "[CHƯA TỐI ƯU] heavy work trên main thread, bitmap="
                            + loadedBitmaps.size() + ", time=" + heavyCost + " ms");
                }

                // ✔ MODE TỐI ƯU – chỉ cập nhật UI nhẹ trong onReceive
                updateNetworkStatus(connected);

                long cost = System.currentTimeMillis() - start;

                String modeLabel = cbHeavyOnReceive.isChecked() ? "CHƯA TỐI ƯU" : "TỐI ƯU";

                // Nếu đang ở MODE TỐI ƯU và mất mạng:
                //   đẩy phần xử lý "bitmap đã load" sang THREAD KHÁC
                if (!cbHeavyOnReceive.isChecked() && !connected) {
                    new Thread(() -> {
                        long heavyStart = System.currentTimeMillis();

                        int dummy = 0;
                        for (Bitmap bmp : loadedBitmaps) {
                            if (bmp != null && !bmp.isRecycled()) {
                                dummy += bmp.getWidth() * bmp.getHeight();
                            }
                        }

                        long heavyCost = System.currentTimeMillis() - heavyStart;

//                        String msgToast = "TỐI ƯU: xử lý "
//                                + loadedBitmaps.size()
//                                + " ảnh trên THREAD riêng ~ " + heavyCost + " ms";

                        // ► Logcat cho phần TỐI ƯU (chạy nền)
                        Log.e(TAG, "[TỐI ƯU] heavy work trên worker thread, bitmap="
                                + loadedBitmaps.size() + ", time=" + heavyCost + " ms");

                        // Báo kết quả lên UI (xử lý nặng đã chạy nền)
//                        runOnUiThread(() ->
//                                Toast.makeText(context, msgToast, Toast.LENGTH_LONG).show()
//                        );
                    }).start();
                }
            }
        };
    }

    private void updateNetworkStatus(boolean connected) {
        if (connected) {
            tvNetworkStatus.setText("ONLINE");
            tvNetworkStatus.setBackgroundColor(0xFFB2FF59);
        } else {
            tvNetworkStatus.setText("OFFLINE");
            tvNetworkStatus.setBackgroundColor(0xFFFF8A80);
        }
    }

    private boolean checkNetworkConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.net.Network network = cm.getActiveNetwork();
            if (network == null) return false;

            NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
            return capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);

        } else {
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isReceiverRegistered) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            if (Build.VERSION.SDK_INT >= 34) {
                registerReceiver(networkReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(networkReceiver, filter);
            }
            isReceiverRegistered = true;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(networkReceiver);
            } catch (Exception ignored) {}
            isReceiverRegistered = false;
        }
    }

    // ==========================================
    // ADAPTER – load ảnh từ web
    // ==========================================
    class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
        private final List<String> urls;

        public ImageAdapter(List<String> urls) {
            this.urls = urls;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_image, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.ivImage.setImageBitmap(null);
            holder.tvLoadTime.setText("Loading...");

            // Nếu đang offline → không cố tải ảnh nữa
            if (isOffline) {
                holder.tvLoadTime.setText("Offline - không tải được ảnh mới");
                return;
            }

            new ImageLoaderTask(holder.ivImage, holder.tvLoadTime, urls.get(position)).execute();
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
                ivImage    = itemView.findViewById(R.id.ivItemImage);
                tvLoadTime = itemView.findViewById(R.id.tvLoadTime);
            }
        }
    }

    // Load ảnh không cache, nhưng LƯU bitmap vào loadedBitmaps
    class ImageLoaderTask extends AsyncTask<Void, Void, Bitmap> {
        private final ImageView imageView;
        private final TextView tvTime;
        private final String url;
        private final long startTime;

        public ImageLoaderTask(ImageView imageView, TextView tvTime, String url) {
            this.imageView = imageView;
            this.tvTime = tvTime;
            this.url = url;
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
                imageView.setImageBitmap(result);
                tvTime.setText("Net load: " + (System.currentTimeMillis() - startTime) + " ms");

                // ✅ LƯU bitmap đã tải vào danh sách, để khi mất mạng xử lý tiếp
                loadedBitmaps.add(result);
            } else {
                tvTime.setText("Load fail");
            }
        }
    }
}
