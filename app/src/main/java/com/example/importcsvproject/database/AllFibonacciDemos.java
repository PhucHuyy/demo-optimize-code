package com.example.importcsvproject.database;

import android.util.Log;
import android.util.SparseArray;
import java.math.BigInteger;

public class AllFibonacciDemos {

    private static final String TAG = "FibonacciDemo";

    // --- 1. Đệ quy cơ bản (Hình 1.2) ---
    public static long computeRecursively(int n) {
        if (n > 1) {
            return computeRecursively(n - 2) + computeRecursively(n - 1);
        }
        return n;
    }

    // --- 2. Đệ quy "tối ưu" một phần (Hình 2.1) ---
    // (Vẫn là đệ quy, chỉ giảm 1 lệnh gọi)
    public static long computeRecursivelyWithLoop(int n) {
        if (n > 1) {
            long result = 1;
            do {
                result += computeRecursivelyWithLoop(n - 2);
                n--;
            } while (n > 1);
            return result;
        }
        return n;
    }

    // --- 3. Vòng lặp cơ bản (Hình 2.2) ---
    public static long computeIteratively(int n) {
        if (n > 1) {
            long a = 0, b = 1;
            do {
                long tmp = b;
                b += a;
                a = tmp;
            } while (--n > 1);
            return b;
        }
        return n;
    }

    // --- 4. Vòng lặp tối ưu (Hình 2.3) ---
    private static long computeIterativelyFaster(int n) {
        if (n > 1) {
            long a, b = 1;
            n--;
            a = n & 1; // n & 1 là phép toán bit, tương đương (n % 2)
            n /= 2;
            while (n-- > 0) {
                a += b;
                b += a;
            }
            return b;
        }
        return n;
    }

    // --- 5. Vòng lặp tối ưu với BigInteger (Hình 2.4) ---
    public static BigInteger computeIterativelyFasterUsingBigInteger(int n) {
        if (n > 1) {
            BigInteger a, b = BigInteger.ONE;
            n--;
            a = BigInteger.valueOf(n & 1);
            n /= 2;
            while (n-- > 0) {
                a = a.add(b);
                b = b.add(a);
            }
            return b;
        }
        return (n == 0) ? BigInteger.ZERO : BigInteger.ONE;
    }

    // --- 6. Đệ quy (BigInt + Primitive) (Hình 2.6) ---
    public static BigInteger computeRecursivelyFasterUsingBigIntegerAndPrimitive(int n) {
        if (n > 92) { // 92 là số Fib lớn nhất mà 'long' chứa được [cite: 147]
            int m = (n / 2) + (n & 1);
            BigInteger fM = computeRecursivelyFasterUsingBigIntegerAndPrimitive(m);
            BigInteger fM_1 = computeRecursivelyFasterUsingBigIntegerAndPrimitive(m - 1);

            if ((n & 1) == 1) { // n là số lẻ
                // F(2n-1) = F(n)^2 + F(n-1)^2
                return fM.pow(2).add(fM_1.pow(2));
            } else { // n là số chẵn
                // F(2n) = (2*F(n-1) + F(n)) * F(n)
                return fM_1.shiftLeft(1).add(fM).multiply(fM); // shiftLeft(1) là nhân 2
            }
        }
        // Dùng hàm vòng lặp long nhanh cho n <= 92
        return BigInteger.valueOf(computeIterativelyFaster(n));
    }

    // --- 7. Đệ quy (BigInt + Primitive + Cache) (Hình 2.9) ---
    public static BigInteger computeRecursivelyWithCache(int n) {
        // Hàm public bên ngoài để khởi tạo cache
        SparseArray<BigInteger> cache = new SparseArray<>();
        return computeRecursivelyWithCache(n, cache);
    }

    private static BigInteger computeRecursivelyWithCache(int n, SparseArray<BigInteger> cache) {
        if (n > 92) {
            BigInteger fN = cache.get(n); // Thử lấy từ cache
            if (fN == null) { // Nếu không có trong cache, tính toán
                int m = (n / 2) + (n & 1);
                BigInteger fM = computeRecursivelyWithCache(m, cache);
                BigInteger fM_1 = computeRecursivelyWithCache(m - 1, cache);

                if ((n & 1) == 1) {
                    fN = fM.pow(2).add(fM_1.pow(2));
                } else {
                    fN = fM_1.shiftLeft(1).add(fM).multiply(fM);
                }
                cache.put(n, fN); // Lưu kết quả vào cache
            }
            return fN; // Trả về kết quả
        }
        // Dùng hàm vòng lặp long nhanh cho n <= 92
        return BigInteger.valueOf(computeIterativelyFaster(n));
    }


    /**
     * Phương thức này sẽ chạy tất cả các demo và in kết quả ra Logcat.
     * Nó PHẢI được gọi từ một luồng nền (background thread).
     */
    public static void runAllDemosOnAndroid() {
        Log.d(TAG, "===== BẮT ĐẦU FIBONACCI DEMO =====");

        // --- Demo A: So sánh các hàm dùng 'long' với n nhỏ (ví dụ: 35) ---
        // 'n' đủ lớn để thấy đệ quy chậm, nhưng đủ nhỏ để 'long' không tràn
        int n_small = 30;
        Log.d(TAG, "--- Demo A: Chạy với n = " + n_small + " (kiểu long) ---");

        long startTime = System.currentTimeMillis();
        long res1 = computeRecursively(n_small); // [cite: 91]
        Log.d(TAG, String.format("1. Đệ quy (Hình 1.2):      Kết quả=%d, Thời gian=%d ms", res1, (System.currentTimeMillis() - startTime)));

        startTime = System.currentTimeMillis();
        long res2 = computeRecursivelyWithLoop(n_small); // [cite: 129]
        Log.d(TAG, String.format("2. Đệ quy tối ưu (Hình 2.1): Kết quả=%d, Thời gian=%d ms", res2, (System.currentTimeMillis() - startTime)));

        startTime = System.currentTimeMillis();
        long res3 = computeIteratively(n_small); // [cite: 135]
        Log.d(TAG, String.format("3. Vòng lặp (Hình 2.2):      Kết quả=%d, Thời gian=%d ms", res3, (System.currentTimeMillis() - startTime)));

        startTime = System.currentTimeMillis();
        long res4 = computeIterativelyFaster(n_small); // [cite: 143]
        Log.d(TAG, String.format("4. Vòng lặp tối ưu (Hình 2.3):  Kết quả=%d, Thời gian=%d ms", res4, (System.currentTimeMillis() - startTime)));


        // --- Demo B: So sánh các hàm dùng 'BigInteger' với n lớn (ví dụ: 50000) ---
        int n_large = 50000;
        Log.d(TAG, "--- Demo B: Chạy với n = " + n_large + " (kiểu BigInteger) ---");

        startTime = System.currentTimeMillis();
        BigInteger res5 = computeIterativelyFasterUsingBigInteger(n_large); // [cite: 152]
        Log.d(TAG, String.format("5. Vòng lặp BigInt (Hình 2.4):      Thời gian=%d ms", (System.currentTimeMillis() - startTime)));

        startTime = System.currentTimeMillis();
        BigInteger res6 = computeRecursivelyFasterUsingBigIntegerAndPrimitive(n_large); // [cite: 179]
        Log.d(TAG, String.format("6. Đệ quy BigInt+Primitive (Hình 2.6): Thời gian=%d ms", (System.currentTimeMillis() - startTime)));

        startTime = System.currentTimeMillis();
        BigInteger res7 = computeRecursivelyWithCache(n_large); // [cite: 206]
        Log.d(TAG, String.format("7. Đệ quy BigInt+Cache (Hình 2.9):   Thời gian=%d ms", (System.currentTimeMillis() - startTime)));

        Log.d(TAG, "===== KẾT THÚC FIBONACCI DEMO =====");
    }
}