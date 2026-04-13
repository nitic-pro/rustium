package pro.nitic.rustium.rustium.native_rust;

import java.lang.foreign.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.invoke.MethodHandle;

/**
 * FFM API (Project Panama) を介してRustネイティブライブラリを直接呼び出すクライアント。
 * OCI (ARM) と DigitalOcean (x86_64) を相互補完。
 */
public class RustNativeOffloader {
    
    private static final SymbolLookup LIBRUST;
    private static final Linker LINKER;
    private static final MethodHandle COMPUTE_LIGHTING;

    static {
        // アーキテクチャに合わせてロードされるネイティブ .so (.dll) を自動判別
        String osArch = System.getProperty("os.arch"); // amd64 または aarch64
        String libName = osArch.contains("aarch64") 
            ? "librust_engine_aarch64.so" 
            : "librust_engine_x86_64.so";
            
        Path libPath = Paths.get("native_libs", libName);
        
        System.load(libPath.toAbsolutePath().toString());
        
        LINKER = Linker.nativeLinker();
        LIBRUST = SymbolLookup.loaderLookup();
        
        // メソッド "calculate_chunk_lighting_bulk" の登録 (ポインタ, サイズを引数に取る想定)
        MemorySegment funcAddr = LIBRUST.find("calculate_chunk_lighting_bulk").orElseThrow();
        COMPUTE_LIGHTING = LINKER.downcallHandle(
            funcAddr,
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );
    }

    /**
     * Chunkのライティング計算などをRust関数に丸投げ（バッファオフロード）する。
     * GCの影響を一切受けずに、C/Rust環境上で最高峰のハードウェア最適化処理を行う。
     */
    public static void offloadLightingCalculation(MemorySegment chunkData, int dataSize) {
        try {
            if (COMPUTE_LIGHTING != null) {
                COMPUTE_LIGHTING.invokeExact(chunkData, dataSize);
            }
        } catch (Throwable e) {
            throw new RuntimeException("A critical error occurred connecting to Rustium native engine.", e);
        }
    }
    
    public static void init() {
    	// Dummy init to trigger static block
    }
}
