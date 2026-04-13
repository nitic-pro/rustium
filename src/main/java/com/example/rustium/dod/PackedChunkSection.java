package com.example.rustium.dod;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * データ指向設計 (Data-Oriented Design)
 * チャンクの4096ブロック(16x16x16)のステートを、オブジェクトのポインタ参照ではなく
 * 連続したプリミティヴな一次元int配列として保持し、CPUキャッシュヒット率を極限まで高める。
 */
public class PackedChunkSection {
    // 16 * 16 * 16 = 4096 ブロック分の状態IDを1次元配列にパック
    private final int[] blocks = new int[4096];

    // CPUアーキテクチャに合わせて、利用可能な最大長のSIMDレジスタ（通常256bit AVX2 または 512bit AVX-512）を自動選択
    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    /**
     * SIMD (単一命令複数データ) 拡張命令を用いた超高速・空気判定。
     * ループの回数を劇的に減らし、1クロックで複数ブロック(8〜16個)を同時に判定する。
     */
    public boolean hasOnlyAirVectorized() {
        int i = 0;
        int bound = SPECIES.loopBound(blocks.length);

        // Vector APIのゼロベクトル (全て0=空気ブロックのIDが0と仮定)
        IntVector zeroVector = IntVector.zero(SPECIES);

        // レジスタ幅単位でのバルク一括比較
        for (; i < bound; i += SPECIES.length()) {
            IntVector v = IntVector.fromArray(SPECIES, blocks, i);
            
            // v != zeroVector となる要素が1つでもあるか？（つまり空気以外のブロックがあるか）
            if (v.compare(jdk.incubator.vector.VectorOperators.NE, zeroVector).anyTrue()) {
                return false; // 空気以外のブロックが存在する
            }
        }

        // 余りの端数処理（通常4096はSIMD幅で割り切れるためここには来ないが、念のため）
        for (; i < blocks.length; i++) {
            if (blocks[i] != 0) {
                return false;
            }
        }

        return true; // 全て空気
    }
}
