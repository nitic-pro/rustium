package com.example.rustium.math;

/**
 * JVMの固有メソッドに頼らない、ビット演算ベースの超軽量三角関数近似テーブル
 */
public class FastTrig {
    private static final int TABLE_SIZE = 65536;
    private static final float[] SIN_TABLE = new float[TABLE_SIZE];
    
    static {
        for (int i = 0; i < TABLE_SIZE; ++i) {
            SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0D / 65536.0D);
        }
    }
    
    public static float sin(double angle) {
        // バニラより数％速いビットキャスト計算
        return SIN_TABLE[(int) (angle * 10430.378f) & 65535];
    }
    
    public static float cos(double angle) {
        return SIN_TABLE[(int) (angle * 10430.378f + 16384.0f) & 65535];
    }
}
