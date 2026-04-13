package pro.nitic.rustium.rustium.math;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorOperators;

/**
 * ベクトル演算（SIMD）を活用した最適化計算モジュール。
 * 環境に応じてAVX2やNEON等の命令セットが自動選択（JITコンパイル）されます。
 */
public class VectorMathOptimizations {
    // 256bit長（AVX2や特定のNEON向け）など環境に最適なVector Speciesを自動取得
    private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;

    /**
     * 高速な距離計算・周辺ブロック更新などのバルク処理（SIMD版）
     */
    public static void computeDistancesVectorized(float[] xArray, float[] yArray, float[] zArray, float[] outDistances, int length) {
        int i = 0;
        int loopBound = SPECIES.loopBound(length);

        for (; i < loopBound; i += SPECIES.length()) {
            FloatVector xVec = FloatVector.fromArray(SPECIES, xArray, i);
            FloatVector yVec = FloatVector.fromArray(SPECIES, yArray, i);
            FloatVector zVec = FloatVector.fromArray(SPECIES, zArray, i);
            
            // x^2 + y^2 + z^2
            FloatVector distVec = xVec.mul(xVec).add(yVec.mul(yVec)).add(zVec.mul(zVec));
            // sqrt(distVec)
            FloatVector sqrtVec = distVec.lanewise(VectorOperators.SQRT);
            
            sqrtVec.intoArray(outDistances, i);
        }

        // 残像処理 (スカラ)
        for (; i < length; i++) {
            outDistances[i] = (float) Math.sqrt(xArray[i] * xArray[i] + yArray[i] * yArray[i] + zArray[i] * zArray[i]);
        }
    }
}