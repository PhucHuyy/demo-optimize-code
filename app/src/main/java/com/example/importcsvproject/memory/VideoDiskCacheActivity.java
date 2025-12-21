package com.example.importcsvproject.memory;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import com.example.importcsvproject.R;

import java.io.File;

@UnstableApi
public class VideoDiskCacheActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private TextView tvStatus, tvCacheSize;
    private ProgressBar pbCache;
    private Button btnLoadVideo, btnClearCache;

    // Singleton Cache (Trong thực tế nên để ở Application class)
    private static SimpleCache simpleCache;
    private final String VIDEO_URL = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"; // Video test chuẩn của Google

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_disk_cache);

        initViews();
        setupCache(); // 1. Cấu hình Disk Cache

        btnLoadVideo.setOnClickListener(v -> playVideo());
        btnClearCache.setOnClickListener(v -> clearCacheData());

        // Vòng lặp cập nhật thông số Cache mỗi giây để hiển thị lên màn hình
        startCacheMonitor();
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
        tvStatus = findViewById(R.id.tvStatus);
        tvCacheSize = findViewById(R.id.tvCacheSize);
        pbCache = findViewById(R.id.pbCache);
        btnLoadVideo = findViewById(R.id.btnLoadVideo);
        btnClearCache = findViewById(R.id.btnClearCache);
    }

    // --- LOGIC CACHE QUAN TRỌNG NHẤT ---
    private void setupCache() {
        if (simpleCache == null) {
            // Định nghĩa thư mục lưu cache: /data/data/pkg/cache/video_cache
            File cacheDir = new File(getCacheDir(), "video_cache");

            // Quy định: Chỉ lưu tối đa 100MB, quá thì xóa cái cũ nhất (LRU)
            LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024);

            // Khởi tạo Database theo dõi cache (cần thiết cho ExoPlayer)
            androidx.media3.database.DatabaseProvider databaseProvider =
                    new androidx.media3.database.StandaloneDatabaseProvider(this);

            simpleCache = new SimpleCache(cacheDir, evictor, databaseProvider);
        }
    }

    private void playVideo() {
        releasePlayer(); // Reset player cũ

        // Tạo Factory dùng CacheDataSource
        // Logic: Đọc Cache -> Nếu thiếu thì tải Http -> Ghi vào Cache
        DataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true);

        CacheDataSource.Factory cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(httpDataSourceFactory)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR); // Nếu lỗi cache thì tải mạng

        // Khởi tạo Player
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(cacheDataSourceFactory))
                .build();

        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(VIDEO_URL));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.play();

        tvStatus.setText("Status: Đang phát...");
        tvStatus.setTextColor(getResources().getColor(android.R.color.holo_blue_light));
    }

    private void clearCacheData() {
        releasePlayer();
        new Thread(() -> {
            // Xóa file vật lý
            File cacheDir = new File(getCacheDir(), "video_cache");
            deleteRecursive(cacheDir);

            runOnUiThread(() -> {
                Toast.makeText(this, "Đã xóa sạch Disk Cache!", Toast.LENGTH_SHORT).show();
                updateCacheSizeUI(); // Cập nhật lại số 0

                // Vì SimpleCache của ExoPlayer rất chặt chẽ,
                // muốn reset hoàn toàn cần khởi động lại app hoặc xử lý phức tạp hơn.
                // Ở demo này ta xóa file vật lý để minh họa.
            });
        }).start();
    }

    // Hàm đệ quy xóa file
    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);
        fileOrDirectory.delete();
    }

    // --- CẬP NHẬT UI (MONITOR) ---
    private void startCacheMonitor() {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateCacheSizeUI();
                handler.postDelayed(this, 1000); // Lặp lại mỗi 1 giây
            }
        }, 1000);
    }

    private void updateCacheSizeUI() {
        if (simpleCache != null) {
            long size = simpleCache.getCacheSpace(); // Lấy dung lượng thực tế đang dùng
            long sizeInMB = size / (1024 * 1024);

            tvCacheSize.setText("Cache Size on Disk: " + sizeInMB + " MB / 100 MB");
            pbCache.setProgress((int) sizeInMB);

            // Logic hiển thị trạng thái nguồn
            if (player != null && player.isPlaying()) {
                // Đây là cách đơn giản để đoán nguồn (thực tế cần listener phức tạp hơn)
                boolean isCached = simpleCache.isCached(VIDEO_URL, 0, 1024); // Check đoạn đầu
                if(isCached && size > 0) {
                    tvStatus.setText("Source: PLAYING FROM DISK (Offline ok)");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                } else {
                    tvStatus.setText("Source: DOWNLOADING & CACHING...");
                    tvStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
                }
            }
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }
}