package pro.nitic.rustium.rustium.ai;

public class FixedPointAITest {
    public static void main(String[] args) {
        System.out.println("=== FixedPointAI Math Debug ===");
        double original = 15.345;
        int fixed = FixedPointAI.toFixed(original);
        double restored = FixedPointAI.toDouble(fixed);
        System.out.println("Original: " + original);
        System.out.println("Fixed (Int): " + fixed);
        System.out.println("Restored: " + restored);
        System.out.println("Error: " + Math.abs(original - restored));
        
        System.out.println("=== Distance Math Debug ===");
        int dist = FixedPointAI.manhattanDist(
            FixedPointAI.toFixed(10.0), FixedPointAI.toFixed(5.0), FixedPointAI.toFixed(0.0),
            FixedPointAI.toFixed(0.0), FixedPointAI.toFixed(0.0), FixedPointAI.toFixed(0.0)
        );
        System.out.println("Distance (Fixed): " + dist);
        System.out.println("Distance (Restored): " + FixedPointAI.toDouble(dist));
    }
}
