package pro.nitic.rustium.rustium;

import java.lang.foreign.*;
import java.util.Random;

public class NoiseBenchmark {
    // 100万頂点のノイズ計算をテスト（巨大なチャンク生成を想定）
    private static final int COUNT = 1_000_000;
    private static final long NOISE_DATA_SIZE = 32;

    public static void main(String[] args) {
        System.out.println("Preparing benchmark for " + COUNT + " terrain noise point calculations...");
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment noiseDataSegment = arena.allocate(COUNT * NOISE_DATA_SIZE);
            MemorySegment pArraySegment = arena.allocate(256);

            Random rand = new Random(42);
            byte[] p = new byte[256];
            rand.nextBytes(p);
            MemorySegment.copy(p, 0, pArraySegment, ValueLayout.JAVA_BYTE, 0, 256);

            for (int i = 0; i < COUNT; i++) {
                long offset = i * NOISE_DATA_SIZE;
                noiseDataSegment.set(ValueLayout.JAVA_DOUBLE, offset, rand.nextDouble() * 100);
                noiseDataSegment.set(ValueLayout.JAVA_DOUBLE, offset + 8, rand.nextDouble() * 100);
                noiseDataSegment.set(ValueLayout.JAVA_DOUBLE, offset + 16, rand.nextDouble() * 100);
            }

            System.out.println("Warming up JVM & Rust FFM Bridge...");
            runJava(noiseDataSegment, COUNT, pArraySegment);
            RustNativeOffloader.computeNoiseBulk(noiseDataSegment, COUNT, pArraySegment);

            System.out.println("--- Starting Benchmark (10 runs) ---");

            // --- Java ---
            long javaTotal = 0;
            int runs = 10;
            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();
                runJava(noiseDataSegment, COUNT, pArraySegment);
                javaTotal += (System.nanoTime() - start);
            }
            double javaAvgMs = (javaTotal / runs) / 1_000_000.0;
            System.out.printf("Vanilla Java (Single Thread): %.2f ms%n", javaAvgMs);

            // --- Rust FFM + Rayon ---
            long rustTotal = 0;
            for (int i = 0; i < runs; i++) {
                long start = System.nanoTime();
                RustNativeOffloader.computeNoiseBulk(noiseDataSegment, COUNT, pArraySegment);
                rustTotal += (System.nanoTime() - start);
            }
            double rustAvgMs = (rustTotal / runs) / 1_000_000.0;
            System.out.printf("RustNative + Rayon (Multi-core): %.2f ms%n", rustAvgMs);

            System.out.println("------------------------------------");
            System.out.printf("Result: FFM Rust is %.2fx FASTER than Java!%n", javaAvgMs / rustAvgMs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Javaでの完全な同等ロジック（ベースライン）
    private static void runJava(MemorySegment data, int count, MemorySegment pArray) {
        byte[] p = new byte[512];
        for (int i = 0; i < 256; i++) {
            p[i] = pArray.get(ValueLayout.JAVA_BYTE, i);
            p[i + 256] = p[i];
        }

        for (int i = 0; i < count; i++) {
            long offset = i * NOISE_DATA_SIZE;
            double x = data.get(ValueLayout.JAVA_DOUBLE, offset);
            double y = data.get(ValueLayout.JAVA_DOUBLE, offset + 8);
            double z = data.get(ValueLayout.JAVA_DOUBLE, offset + 16);

            int floor_x = (int) Math.floor(x);
            int floor_y = (int) Math.floor(y);
            int floor_z = (int) Math.floor(z);

            double frac_x = x - floor_x;
            double frac_y = y - floor_y;
            double frac_z = z - floor_z;

            double u = curver(frac_x);
            double v = curver(frac_y);
            double w = curver(frac_z);

            int a = p[floor_x & 255] & 0xFF;
            int aa = p[(a + floor_y) & 255] & 0xFF;
            int ab = p[(a + floor_y + 1) & 255] & 0xFF;
            int b = p[(floor_x + 1) & 255] & 0xFF;
            int ba = p[(b + floor_y) & 255] & 0xFF;
            int bb = p[(b + floor_y + 1) & 255] & 0xFF;

            double lerp1 = lerp(u, gradDot(p[(aa + floor_z) & 255] & 0xFF, frac_x, frac_y, frac_z),
                                gradDot(p[(ba + floor_z) & 255] & 0xFF, frac_x - 1.0, frac_y, frac_z));
            double lerp2 = lerp(u, gradDot(p[(ab + floor_z) & 255] & 0xFF, frac_x, frac_y - 1.0, frac_z),
                                gradDot(p[(bb + floor_z) & 255] & 0xFF, frac_x - 1.0, frac_y - 1.0, frac_z));
            double lerp3 = lerp(u, gradDot(p[(aa + floor_z + 1) & 255] & 0xFF, frac_x, frac_y, frac_z - 1.0),
                                gradDot(p[(ba + floor_z + 1) & 255] & 0xFF, frac_x - 1.0, frac_y, frac_z - 1.0));
            double lerp4 = lerp(u, gradDot(p[(ab + floor_z + 1) & 255] & 0xFF, frac_x, frac_y - 1.0, frac_z - 1.0),
                                gradDot(p[(bb + floor_z + 1) & 255] & 0xFF, frac_x - 1.0, frac_y - 1.0, frac_z - 1.0));

            double res = lerp(w, lerp(v, lerp1, lerp2), lerp(v, lerp3, lerp4));
            data.set(ValueLayout.JAVA_DOUBLE, offset + 24, res);
        }
    }

    private static double curver(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }
    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
    private static double gradDot(int hash, double x, double y, double z) {
        int h = hash & 15;
        double u = h < 8 ? x : y;
        double v = h < 4 ? y : (h == 12 || h == 14 ? x : z);
        return ((h & 1) != 0 ? -u : u) + ((h & 2) != 0 ? -v : v);
    }
}
