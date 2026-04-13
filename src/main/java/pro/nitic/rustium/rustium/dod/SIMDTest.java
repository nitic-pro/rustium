package pro.nitic.rustium.rustium.dod;

import jdk.incubator.vector.ByteVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

public class SIMDTest {
    private static final VectorSpecies<Byte> SPECIES = ByteVector.SPECIES_PREFERRED;

    public static void main(String[] args) {
        System.out.println("=== SIMD DataLayer Logic Test ===");
        
        // マイクラの DataLayer (2048バイト) をモック
        byte[] emptyData = new byte[2048]; 
        byte[] filledData = new byte[2048];
        filledData[1024] = 15; // 1箇所だけ光を置く

        // 実行検証
        System.out.println("Empty Array Test (Expected = true): " + testIsEmptySIMD(emptyData));
        System.out.println("Filled Array Test (Expected = false): " + testIsEmptySIMD(filledData));
        
        // ベンチマーク (バニラ vs SIMD)
        long start, end;
        int dummy = 0;
        
        // warmup
        for(int i=0; i<100000; i++) {
            if(testIsEmptyVanilla(emptyData)) dummy++;
            if(testIsEmptySIMD(emptyData)) dummy++;
        }

        start = System.nanoTime();
        for(int i=0; i<1000000; i++) {
            if(testIsEmptyVanilla(emptyData)) dummy++;
        }
        end = System.nanoTime();
        System.out.println("Vanilla Time (1,000,000 runs): " + (end - start) / 1000000.0 + " ms");

        start = System.nanoTime();
        for(int i=0; i<1000000; i++) {
            if(testIsEmptySIMD(emptyData)) dummy++;
        }
        end = System.nanoTime();
        System.out.println("SIMD Time (1,000,000 runs): " + (end - start) / 1000000.0 + " ms");
    }

    // バニラの判定ロジック（forループによる全走査）
    private static boolean testIsEmptyVanilla(byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0) return false;
        }
        return true;
    }

    // 組み込んだSIMDアルゴリズム
    private static boolean testIsEmptySIMD(byte[] data) {
        int upperBound = SPECIES.loopBound(data.length);
        ByteVector zeroVec = ByteVector.zero(SPECIES);
        int i = 0;
        for (; i < upperBound; i += SPECIES.length()) {
            ByteVector v = ByteVector.fromArray(SPECIES, data, i);
            if (v.compare(VectorOperators.NE, zeroVec).anyTrue()) {
                return false;
            }
        }
        for (; i < data.length; i++) {
            if (data[i] != 0) return false;
        }
        return true;
    }
}
