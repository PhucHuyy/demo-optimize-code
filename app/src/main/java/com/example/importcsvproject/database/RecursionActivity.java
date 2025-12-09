package com.example.importcsvproject.database;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.importcsvproject.R;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Demo khử đệ quy bằng bài toán DUYỆT CÂY COMMENT.
 *
 * - Dữ liệu: một danh sách comment + reply lồng nhau (cây).
 * - Hai cách duyệt:
 *   + Đệ quy (DFS recursive)
 *   + Vòng lặp + Stack (khử đệ quy, vẫn DFS)
 * - UI hiển thị comment thật theo dạng thread, thụt lề theo depth.
 */
public class RecursionActivity extends AppCompatActivity {

    private EditText etInputN;
    private TextView tvResult;
    private RecyclerView rvComments;
    private CommentAdapter adapter;
    private final List<DisplayComment> displayComments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recursion);

        etInputN = findViewById(R.id.etInputN);
        tvResult = findViewById(R.id.tvResult);
        rvComments = findViewById(R.id.rvComments);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommentAdapter(displayComments);
        rvComments.setAdapter(adapter);

        findViewById(R.id.btnRunRecursive).setOnClickListener(v -> runRecursive());
        findViewById(R.id.btnRunIterative).setOnClickListener(v -> runIterative());
    }

    // ----------------------- MODEL CÂY COMMENT -----------------------

    /**
     * Node comment trong cây.
     */
    static class CommentNode {
        final String author;
        final String content;
        final List<CommentNode> children = new ArrayList<>();

        CommentNode(String author, String content) {
            this.author = author;
            this.content = content;
        }

        void addChild(CommentNode child) {
            children.add(child);
        }
    }

    /**
     * Dữ liệu đã flatten + depth để hiển thị trong RecyclerView.
     */
    static class DisplayComment {
        final String author;
        final String content;
        final int depth;

        DisplayComment(String author, String content, int depth) {
            this.author = author;
            this.content = content;
            this.depth = depth;
        }
    }

    /**
     * Tạo dữ liệu mẫu:
     * - 2 thread comment “thật”
     * - 1 thread có reply rất sâu (theo n nhập), dùng để minh họa nguy cơ StackOverflow khi duyệt bằng đệ quy.
     */
    private List<CommentNode> buildSampleComments(int deepReplyDepth) {
        List<CommentNode> roots = new ArrayList<>();

        if (deepReplyDepth > 0) {
            CommentNode deepRoot = new CommentNode("Admin",
                    "Thread thảo luận về khử đệ quy trong duyệt cây comment.");
            CommentNode current = deepRoot;
            for (int i = 1; i <= deepReplyDepth; i++) {
                CommentNode child = new CommentNode(
                        "User " + i,
                        "Reply level " + i + " – ví dụ comment lồng nhau rất sâu."
                );
                current.addChild(child);
                current = child;
            }
            roots.add(deepRoot);
        }

        return roots;
    }

    // ----------------------- ĐỌC INPUT -----------------------

    private int getDepthInput() {
        String text = etInputN.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            return 10; // mặc định
        }
        try {
            int n = Integer.parseInt(text);
            return Math.max(n, 1);
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    // ----------------------- CHẠY ĐỆ QUY -----------------------

    private void runRecursive() {
        int depth = getDepthInput();
        tvResult.setText("Đang duyệt cây comment bằng ĐỆ QUY...");
        tvResult.setTextColor(Color.BLACK);

        List<CommentNode> roots = buildSampleComments(depth);
        displayComments.clear();

        long startTime = System.currentTimeMillis();
        int count = 0;
        try {
            for (CommentNode root : roots) {
                count += traverseRecursive(root, 0);
            }
            long endTime = System.currentTimeMillis();

            tvResult.setText(
                    "Đệ quy (recursive DFS) – duyệt cây comment thành công!\n" +
                            "Số comment duyệt được: " + count + "\n" +
                            "Độ sâu thread test: " + depth + "\n" +
                            "Thời gian: " + (endTime - startTime) + " ms"
            );
            tvResult.setTextColor(Color.BLACK);
        } catch (StackOverflowError e) {
            tvResult.setText(
                    "CRASH: StackOverflowError!\n" +
                            "Cây comment quá sâu (n quá lớn) → đệ quy dùng quá nhiều Stack."
            );
            tvResult.setTextColor(Color.RED);
        }

        adapter.notifyDataSetChanged();
    }

    private int traverseRecursive(CommentNode node, int depth) {
        if (node == null) return 0;

        displayComments.add(new DisplayComment(node.author, node.content, depth));
        int count = 1;
        for (CommentNode child : node.children) {
            count += traverseRecursive(child, depth + 1);
        }
        return count;
    }

    // ----------------------- CHẠY VÒNG LẶP (KHỬ ĐỆ QUY) -----------------------

    private void runIterative() {
        int depth = getDepthInput();
        tvResult.setText("Đang duyệt cây comment bằng VÒNG LẶP (khử đệ quy)...");
        tvResult.setTextColor(Color.BLUE);

        List<CommentNode> roots = buildSampleComments(depth);
        displayComments.clear();

        long startTime = System.currentTimeMillis();
        int count = traverseIterative(roots);
        long endTime = System.currentTimeMillis();

        tvResult.setText(
                "Vòng lặp + Stack (iterative DFS) – khử đệ quy thành công!\n" +
                        "Số comment duyệt được: " + count + "\n" +
                        "Độ sâu thread test: " + depth + "\n" +
                        "Thời gian: " + (endTime - startTime) + " ms"
        );
        tvResult.setTextColor(Color.BLUE);

        adapter.notifyDataSetChanged();
    }

    private int traverseIterative(List<CommentNode> roots) {
        if (roots == null || roots.isEmpty()) return 0;

        int count = 0;

        // Stack mô phỏng call stack: (node, depth)
        class NodeDepth {
            CommentNode node;
            int depth;

            NodeDepth(CommentNode node, int depth) {
                this.node = node;
                this.depth = depth;
            }
        }

        Deque<NodeDepth> stack = new ArrayDeque<>();

        // Push các root theo thứ tự ngược để khi pop ra vẫn đúng thứ tự
        for (int i = roots.size() - 1; i >= 0; i--) {
            stack.push(new NodeDepth(roots.get(i), 0));
        }

        while (!stack.isEmpty()) {
            NodeDepth nd = stack.pop();
            CommentNode node = nd.node;
            int depth = nd.depth;

            displayComments.add(new DisplayComment(node.author, node.content, depth));
            count++;

            // Push children ngược thứ tự để giữ thứ tự hiển thị
            List<CommentNode> children = node.children;
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(new NodeDepth(children.get(i), depth + 1));
            }
        }

        return count;
    }

    // ----------------------- ADAPTER HIỂN THỊ COMMENT -----------------------

    private class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

        private final List<DisplayComment> data;

        CommentAdapter(List<DisplayComment> data) {
            this.data = data;
        }

        @Override
        public CommentViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new CommentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CommentViewHolder holder, int position) {
            DisplayComment dc = data.get(position);
            holder.tvAuthor.setText(dc.author);
            holder.tvContent.setText(dc.content);

            // Thụt lề theo depth (mỗi level ~ 16dp)
            int paddingStart = dpToPx(dc.depth * 16);
            int top = holder.itemView.getPaddingTop();
            int right = holder.itemView.getPaddingRight();
            int bottom = holder.itemView.getPaddingBottom();
            holder.itemView.setPadding(paddingStart, top, right, bottom);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        class CommentViewHolder extends RecyclerView.ViewHolder {
            TextView tvAuthor;
            TextView tvContent;

            CommentViewHolder(android.view.View itemView) {
                super(itemView);
                tvAuthor = itemView.findViewById(R.id.tvAuthor);
                tvContent = itemView.findViewById(R.id.tvContent);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}
