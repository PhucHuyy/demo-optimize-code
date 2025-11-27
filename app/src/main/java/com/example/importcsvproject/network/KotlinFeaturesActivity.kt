package com.example.importcsvproject.network

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.importcsvproject.R
import kotlin.system.measureNanoTime

class KotlinFeaturesActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kotlin_features)

        tvResult = findViewById(R.id.tvResult)

        // --- SETUP LISTENER ---

        // Kịch bản 1: List vs Sequence
        findViewById<Button>(R.id.btnStandard).setOnClickListener {
            testStandardList()
        }

        findViewById<Button>(R.id.btnSequence).setOnClickListener {
            testSequence()
        }

        // Kịch bản 2: Inline Function
        findViewById<Button>(R.id.btnNoInline).setOnClickListener {
            testNoInline()
        }

        findViewById<Button>(R.id.btnInline).setOnClickListener {
            testInline()
        }
    }

    // ==========================================
    // KỊCH BẢN 1: SEQUENCE (LAZY) VS ITERABLE (EAGER)
    // Bài toán: Tìm số đầu tiên trong 1 triệu số mà (số đó * 2) chia hết cho 3 và 5
    // ==========================================

    private val bigList = (1..1_000_000).toList()

    private fun testStandardList() {
        val time = measureNanoTime {
            // Standard List: Tạo ra list trung gian ở mỗi bước map/filter
            // Nó phải xử lý HẾT 1 triệu phần tử ở bước map, rồi mới qua filter
            val result = bigList
                .map { it * 2 }
                .filter { it % 3 == 0 && it % 5 == 0 }
                .first()
        }
        updateUI("Standard List", time)
    }

    private fun testSequence() {
        val time = measureNanoTime {
            // Sequence: Xử lý từng phần tử qua chuỗi ống (pipe)
            // Tìm thấy số đầu tiên thỏa mãn là DỪNG NGAY, không xử lý các số sau.
            val result = bigList.asSequence()
                .map { it * 2 }
                .filter { it % 3 == 0 && it % 5 == 0 }
                .first()
        }
        updateUI("Sequence (Lazy)", time)
    }

    // ==========================================
    // KỊCH BẢN 2: INLINE FUNCTIONS
    // Bài toán: Gọi hàm Lambda trong vòng lặp 1 triệu lần
    // ==========================================

    // Hàm thường: Mỗi lần gọi tạo ra 1 object Function1 (gây áp lực GC)
    fun normalOperation(action: () -> Unit) {
        action()
    }

    // Hàm Inline: Trình biên dịch copy code action() dán vào nơi gọi (Zero overhead)
    inline fun inlineOperation(action: () -> Unit) {
        action()
    }

    private fun testNoInline() {
        val time = measureNanoTime {
            for (i in 1..1_000_000) {
                normalOperation { /* Do nothing */ }
            }
        }
        updateUI("Normal Function (High Allocation)", time)
    }

    private fun testInline() {
        val time = measureNanoTime {
            for (i in 1..1_000_000) {
                inlineOperation { /* Do nothing */ }
            }
        }
        updateUI("Inline Function (Zero Allocation)", time)
    }

    private fun updateUI(type: String, nanoTime: Long) {
        val ms = nanoTime / 1_000_000
        tvResult.text = "$type:\nThời gian thực thi: $ms ms\n(Kiểm tra Profiler để thấy khác biệt về RAM)"
    }
}