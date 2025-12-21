package com.example.importcsvproject.memory;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.importcsvproject.R;

import java.util.ArrayList;
import java.util.List;

public class BitmapDownsamplingActivity extends AppCompatActivity {

    private TextView tvInfo;
    private RecyclerView rvFriends;
    private FriendAdapter adapter;
    private final List<Friend> friends = new ArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Kích thước mong muốn cho avatar (px)
    private static final int REQ_WIDTH = 200;
    private static final int REQ_HEIGHT = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitmap_downsampling);

        tvInfo = findViewById(R.id.tvInfo);
        rvFriends = findViewById(R.id.rvFriends);
        Button btnOriginal = findViewById(R.id.btnLoadOriginal);
        Button btnOptimized = findViewById(R.id.btnLoadOptimized);

        // Khởi tạo danh sách bạn bè giả lập
        initFriends();

        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter(friends);
        rvFriends.setAdapter(adapter);

        btnOriginal.setOnClickListener(v -> loadAvatars(false));
        btnOptimized.setOnClickListener(v -> loadAvatars(true));
    }

    private void initFriends() {
        friends.clear();
        friends.add(new Friend("An"));
        friends.add(new Friend("Bình"));
        friends.add(new Friend("Chi"));
        friends.add(new Friend("Dũng"));
        friends.add(new Friend("Giang"));
        friends.add(new Friend("Hà"));
        friends.add(new Friend("Khánh"));
        friends.add(new Friend("Lan"));
        friends.add(new Friend("Minh"));
        friends.add(new Friend("Ngọc"));
        // Bạn có thể thêm nhiều bạn nữa cho list dài hơn
    }

    /**
     * Load avatar cho tất cả bạn bè.
     *
     * @param optimized true  -> dùng downsampling
     *                  false -> load nguyên bản
     */
    private void loadAvatars(boolean optimized) {
        // Dọn bitmap cũ (nếu có) để tránh rò rỉ
        clearBitmaps();

        tvInfo.setText(optimized
                ? "Đang load avatar đã downsample..."
                : "Đang load avatar nguyên bản (có thể tốn RAM)...");

        new Thread(() -> {
            long totalTime = 0;
            long totalBytes = 0;

            for (Friend f : friends) {
                long start = System.currentTimeMillis();
                Bitmap bm;

                if (optimized) {
                    bm = decodeSampledBitmapFromResource(
                            getResources(),
                            R.drawable.img_2,
                            REQ_WIDTH,
                            REQ_HEIGHT
                    );
                } else {
                    BitmapFactory.Options opt = new BitmapFactory.Options();
                    opt.inScaled = false;
                    //opt.inPreferredConfig = Bitmap.Config.RGB_565;
                    bm = BitmapFactory.decodeResource(
                            getResources(),
                            R.drawable.img_2,
                            opt
                    );
                }

                long end = System.currentTimeMillis();
                long time = end - start;

                f.avatar = bm;
                f.loadTimeMs = time;

                if (bm != null) {
                    totalBytes += bm.getAllocationByteCount();
                }
                totalTime += time;
            }

            final long finalTotalTime = totalTime;
            final long finalTotalBytes = totalBytes;

            mainHandler.post(() -> {
                adapter.notifyDataSetChanged();

                float mb = finalTotalBytes / (1024f * 1024f);
                String mode = optimized ? "ĐÃ DOWNSAMPLE" : "NGUYÊN BẢN";

                tvInfo.setText(
                        "Chế độ: " + mode +
                                "\nSố bạn: " + friends.size() +
                                "\nTổng thời gian load: " + finalTotalTime + " ms" +
                                String.format("\nTổng bộ nhớ avatar: %.2f MB", mb)
                );
            });
        }).start();
    }

    private void clearBitmaps() {
        for (Friend f : friends) {
            if (f.avatar != null && !f.avatar.isRecycled()) {
                f.avatar.recycle();
            }
            f.avatar = null;
            f.loadTimeMs = 0;
        }
        System.gc();
        adapter.notifyDataSetChanged();
    }

    // ================== MODEL & ADAPTER ==================

    private static class Friend {
        final String name;
        Bitmap avatar;
        long loadTimeMs;

        Friend(String name) {
            this.name = name;
        }
    }

    private class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

        private final List<Friend> data;

        FriendAdapter(List<Friend> data) {
            this.data = data;
        }

        @Override
        public FriendViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_friend, parent, false);
            return new FriendViewHolder(view);
        }

        @Override
        public void onBindViewHolder(FriendViewHolder holder, int position) {
            Friend f = data.get(position);
            holder.tvName.setText(f.name);

            if (f.avatar != null) {
                holder.ivAvatar.setImageBitmap(f.avatar);
                long sizeKb = f.avatar.getAllocationByteCount() / 1024;
                holder.tvInfoItem.setText(
                        "Time: " + f.loadTimeMs + " ms | Size: " + sizeKb + " KB"
                );
            } else {
                holder.ivAvatar.setImageResource(0);
                holder.tvInfoItem.setText("Chưa load avatar");
            }
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class FriendViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView ivAvatar;
            android.widget.TextView tvName;
            android.widget.TextView tvInfoItem;

            FriendViewHolder(android.view.View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
                tvInfoItem = itemView.findViewById(R.id.tvInfoItem);
            }
        }
    }

    // ================== HÀM DOWNSAMPLING ==================

    /**
     * Decode bitmap theo kích thước mong muốn, tránh load full-size.
     */
    private Bitmap decodeSampledBitmapFromResource(Resources res, int resId,
                                                   int reqWidth, int reqHeight) {
        // Bước 1: chỉ đọc kích thước (không load bitmap vào RAM)
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);

        // Bước 2: tính inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Bước 3: decode thật với inSampleSize đã tính
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }

    /**
     * Tính inSampleSize là lũy thừa của 2 sao cho ảnh nhỏ lại gần với kích thước yêu cầu.
     */
    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Lặp nhân 2 cho tới khi kích thước chia cho inSampleSize vẫn còn >= req
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
