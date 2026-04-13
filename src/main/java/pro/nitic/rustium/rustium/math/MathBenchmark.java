package pro.nitic.rustium.rustium.math;

public class MathBenchmark {
    private static final float[] SIN_TABLE = new float[65536];
    
    static {
        for (int i = 0; i < 65536; ++i) {
            SIN_TABLE[i] = (float) Math.sin((double) i * Math.PI * 2.0D / 65536.0D);
        }
    }
    
    // Vanilla implementation simulation
    public static float vanillaSin(double d) {
        return SIN_TABLE[(int)(d * 10430.378350470453D) & 65535];
    }
    
    public static float fastSin(double d) {
        return SIN_TABLE[(int)(d * 10430.378f) & 65535];
    }

    public static void main(String[] args) {
        long start, end;
        float discard = 0;
        
        // Warmup
        for(int i=0; i<10000000; i++) {
            discard += vanillaSin(i * 0.01);
            discard += fastSin(i * 0.01);
        }
        
        start = System.nanoTime();
        for(int i=0; i<100000000; i++) {
            discard += vanillaSin(i * 0.01);
        }
        end = System.nanoTime();
        System.out.println("Vanilla Time: " + (end - start) / 1000000.0 + " ms");
        
        start = System.nanoTime();
        for(int i=0; i<100000000; i++) {
            discard += fastSin(i * 0.01);
        }
        end = System.nanoTime();
        System.out.println("FastTrig Time: " + (end - start) / 1000000.0 + " ms");
        
        System.out.println("Discard (prevents optimization): " + discard);
    }
}
