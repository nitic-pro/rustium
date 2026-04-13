package com.example.rustium;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

public class RustNativeOffloader {
    public static final Arena ARENA = Arena.ofShared();
    private static MethodHandle CALCULATE_TERRAIN_NOISE_BULK;
    private static MethodHandle CALCULATE_TERRAIN_NOISE_SINGLE;

    // Struct size: x(8), y(8), z(8), result(8) = 32 bytes
    public static final long NOISE_DATA_SIZE = 32;

    static {
        try {
            String osName = System.getProperty("os.name").toLowerCase();
            String libExtension = osName.contains("win") ? ".dll" : osName.contains("mac") ? ".dylib" : ".so";
            String libPrefix = osName.contains("win") ? "rust_engine" : "librust_engine";
            Path libPath = Path.of(System.getProperty("user.dir"), "rust_native", "target", "release", libPrefix + libExtension).toAbsolutePath();

            if (!libPath.toFile().exists()) {
                System.err.println("CRITICAL ERROR: Native Rust Library not found at " + libPath);
                System.err.println("Please build the Rust library using 'cargo build --release' in the rust_native folder.");
            } else {
                SymbolLookup lookup = SymbolLookup.libraryLookup(libPath, Arena.global());
                Linker linker = Linker.nativeLinker();

                MemorySegment symbolBulk = lookup.find("calculate_terrain_noise_bulk").orElseThrow();
                CALCULATE_TERRAIN_NOISE_BULK = linker.downcallHandle(symbolBulk, FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS
                ));

                MemorySegment symbolSingle = lookup.find("calculate_terrain_noise_single").orElseThrow();
                CALCULATE_TERRAIN_NOISE_SINGLE = linker.downcallHandle(symbolSingle, FunctionDescriptor.of(
                        ValueLayout.JAVA_DOUBLE, 
                        ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE, ValueLayout.JAVA_DOUBLE,
                        ValueLayout.ADDRESS
                ));

                System.out.println("SUCCESS: Rust FFM Terrain Noise Bound Linked. OS: " + osName);
            }
        } catch (Exception e) {
            System.err.println("Failed to bind Rust FFM Offloader:");
            e.printStackTrace();
        }
    }

    public static void computeNoiseBulk(MemorySegment noiseDataArray, int count, MemorySegment pArray) {
        if (CALCULATE_TERRAIN_NOISE_BULK == null) return;
        try {
            CALCULATE_TERRAIN_NOISE_BULK.invokeExact(noiseDataArray, count, pArray);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static double computeNoiseSingle(double x, double y, double z, MemorySegment pArray) {
        if (CALCULATE_TERRAIN_NOISE_SINGLE == null) return 0.0;
        try {
            return (double) CALCULATE_TERRAIN_NOISE_SINGLE.invokeExact(x, y, z, pArray);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
