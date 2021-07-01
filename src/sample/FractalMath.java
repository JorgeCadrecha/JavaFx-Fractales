package sample;

import points2d.Vec2df;

/**
 * This class contains the static methods
 * what do the math for the fractal
 */
public class FractalMath {

    public static int naiveCal(float x, float y, double xScale, double yScale, float fTop, float fLeft, int iterations) {
        double realC = x * xScale + fTop;
        double compC = y * yScale + fLeft;
        double realZ = 0.0;
        double compZ = 0.0;

        double realZ2;
        double compZ2;

        double mod = 0;

        int n = 0;
        while ( mod < 4.0 && n < iterations ) {

            // Operación: z = z * z + c
            realZ2 = realZ * realZ - compZ * compZ;
            compZ2 = 2 * realZ * compZ;

            realZ = realZ2;
            compZ = compZ2;

            realZ += realC;
            compZ += compC;

            n++;
            mod = (realZ * realZ + compZ * compZ);
        }

        return n;
    }

}
