package pro.nitic.rustium.rustium.ai;

/**
 * 浮動小数点 (float / double) の重い演算を整数化(Integer/Fixed-Point)して行うユーティリティ。
 * Integer は CPU上での命令レイテンシが短く、SIMD や分岐予測でも極めて有利。
 * Floatによる誤差丸めの発生も防ぐため、Minecraftのエンティティ動作でのラグの大半を削れる。
 */
public class FixedPointAI {

    /** 
     * 1024 (2^10) をスケール(分母)にする。1.0d == 1024 
     * ビットシフトで等倍、乗算、除算がすべて行える。
     */
    public static final int SHIFT = 10;
    public static final int SCALE = 1 << SHIFT;
    public static final int HALF = SCALE >> 1;

    // ----- [Double <-> Int 変換] -----
    
    public static int toFixed(double val) {
        return (int) Math.round(val * SCALE);
    }

    public static double toDouble(int fixed) {
        return (double) fixed / SCALE;
    }

    // ----- [演算系] -----

    // 乗算 (a * b)
    public static int mul(int a, int b) {
        // オーバーフローを避けるため long 経由 (32bit+32bitの演算ならAVXで瞬殺)
        return (int) (((long) a * b) >> SHIFT);
    }
    
    // 除算 (a / b)
    public static int div(int a, int b) {
        if (b == 0) return 0; // 防護処理
        return (int) ((((long) a) << SHIFT) / b);
    }

    // Fast 距離 (マンハッタン距離：sqrtを省く最も早い代替実装)
    public static int manhattanDist(int ax, int ay, int az, int bx, int by, int bz) {
        return Math.abs(ax - bx) + Math.abs(ay - by) + Math.abs(az - bz);
    }

    // チャンク境界座標の計算 (X座標を16で割る操作 => ビットシフトの組み合わせ)
    public static int toChunkCoord(int fixed) {
        // 10ビット(小数) + 4ビット(16ブロック分) の右シフトでチャンクが求まる。
        return fixed >> (SHIFT + 4);
    }
}
